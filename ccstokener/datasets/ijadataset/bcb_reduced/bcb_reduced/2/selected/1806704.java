package recursos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class Agenda {

    private Connection con = null;

    private Statement stmt = null;

    public Agenda() {
        con = Conecta.getInstance();
    }

    public String montaAgendaPaciente(int dia, int mes, int ano, String codcli, int cod_proced, String cod_empresa) {
        Statement stmt = null;
        String sql = "";
        GregorianCalendar inicio = null;
        String resp = "";
        String consultas = "";
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            inicio = Util.toTime("00:00:00", dia, mes, ano);
            String data = Util.formataDataInvertida(Util.formataData(dia, mes, ano));
            int dia_semana = inicio.get(Calendar.DAY_OF_WEEK);
            String feriado = new AgendadoMedico().ehFeriado(inicio);
            if (feriado != null) {
                resp = "<table cellspacing=0 cellpadding=0 class='table' width='98%'><tr>\n";
                resp += "<td class='tdMedium' align='center'>Agenda Bloqueada</td>\n";
                resp += "</tr><tr>\n";
                resp += "<td class='tdLight' align='center'>" + feriado + "</td>\n";
                resp += "</tr></table>\n";
                return resp;
            }
            sql = "SELECT DISTINCT(profissional.prof_reg), LEFT(profissional.nome,15) as nome15, profissional.nome, ";
            sql += "agendamedico.hora_inicio, agendamedico.hora_fim, profissional.tempoconsulta ";
            sql += "FROM (prof_esp INNER JOIN profissional ON prof_esp.prof_reg ";
            sql += "= profissional.prof_reg) INNER JOIN (agendamedico INNER ";
            sql += "JOIN agenda_procedimento ON agendamedico.agenda_id = ";
            sql += "agenda_procedimento.agenda_id) ON prof_esp.prof_reg = ";
            sql += "agendamedico.prof_reg ";
            sql += "WHERE profissional.ativo = 'S' AND profissional.exibir = 'S' ";
            sql += "AND agendamedico.dia_semana=" + dia_semana + " ";
            sql += "AND agenda_procedimento.cod_proced=" + cod_proced + " ";
            sql += "AND CONCAT(agendamedico.vigencia, agendamedico.prof_reg) IN (";
            sql += "SELECT CONCAT(MAX(agendamedico.vigencia), agendamedico.prof_reg) ";
            sql += "FROM agendamedico WHERE vigencia <= '" + data + "' GROUP BY prof_reg) ";
            sql += " ORDER BY nome";
            rs = stmt.executeQuery(sql);
            resp = "<div style='width:480px;height:38px; overflow: auto'>";
            resp += "<table cellspacing=0 cellpadding=0 class='table' width='98%'><tr>\n";
            int contaprofissionais = 0;
            String prof_reg_old = "";
            while (rs.next()) {
                resp += "<td id='cabecalho" + rs.getString("profissional.prof_reg") + "' class='tdMedium' style='padding:0px'><a title='Inserir Encaixe' href=\"Javascript:insereencaixe('" + rs.getString("prof_reg") + "','" + rs.getString("nome") + "')\"6><img src='images/insert.gif' border='0'></a>&nbsp;<a title='" + rs.getString("nome") + "' href=\"Javascript:escolheProfissional('" + rs.getString("prof_reg") + "')\">" + rs.getString("nome15") + "...</a></td>\n";
                contaprofissionais++;
                prof_reg_old = rs.getString("profissional.prof_reg");
            }
            resp += "</tr>\n</table>\n</div>";
            if (contaprofissionais == 1) {
                resp += "<input type='hidden' id='prof_reg_unico' value='" + prof_reg_old + "'>";
            } else {
                resp += "<input type='hidden' id='prof_reg_unico' value=''>";
            }
            resp += "<table cellspacing=0 cellpadding=0 width='480'><tr><td width='100%'>\n";
            rs.beforeFirst();
            while (rs.next()) {
                consultas += getAgendaMedico(rs.getString("profissional.prof_reg"), rs.getString("agendamedico.hora_inicio"), rs.getString("agendamedico.hora_fim"), rs.getInt("tempoconsulta"), dia, mes, ano);
            }
            resp += "</td>\n</tr>\n</table>\n";
        } catch (Exception e) {
            resp = e.toString() + "SQL=" + sql;
        }
        return resp + consultas;
    }

    private String getAgendaMedico(String prof_reg, String inicioMedico, String fimMedico, int intervalo, int dia, int mes, int ano) {
        String resp = "";
        resp += "<div id='medico" + prof_reg + "' style='display:none'>";
        resp += "<table cellspacing='0' cellpadding='0' class='table' width='100%'>";
        GregorianCalendar gc_iniciomedico, gc_fimmedico;
        gc_iniciomedico = Util.toTime(inicioMedico, dia, mes, ano);
        gc_fimmedico = Util.toTime(fimMedico, dia, mes, ano);
        resp += getAgenda(gc_iniciomedico, prof_reg, intervalo, -1);
        while (Util.emMinutos(gc_iniciomedico) <= Util.emMinutos(Util.addMinutos(gc_fimmedico, -intervalo))) {
            resp += getAgenda(gc_iniciomedico, prof_reg, intervalo, 0);
            gc_iniciomedico = Util.addMinutos(gc_iniciomedico, intervalo);
        }
        resp += getAgenda(gc_fimmedico, prof_reg, intervalo, 1);
        resp += "</table></div>";
        return resp;
    }

    private String toString(GregorianCalendar hora) {
        int h, m;
        String resp, hs = "", ms = "";
        try {
            h = hora.get(Calendar.HOUR_OF_DAY);
            m = hora.get(Calendar.MINUTE);
            if (h < 10) {
                hs = "0";
            }
            if (m < 10) {
                ms = "0";
            }
            resp = hs + h + ":" + ms + m;
            return resp;
        } catch (Exception e) {
            return "Erro na convers�o para String: " + e.toString();
        }
    }

    public String toString(int dia, int mes, int ano) {
        String sd = "", sm = "";
        if (dia < 10) {
            sd = "0";
        }
        if (mes < 10) {
            sm = "0";
        }
        return (sd + dia + "/" + sm + mes + "/" + ano);
    }

    public String getAgenda(GregorianCalendar data, String prof_reg, int intervalo, int encaixe) {
        String resp = "", estrutura = "";
        Statement stmt = null;
        String eh_encaixe = "";
        String eh_retorno = "";
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            String sql = "";
            ResultSet rs = null;
            sql += "SELECT paciente.nome, paciente.codcli, agendamento.hora, grupoprocedimento.";
            sql += "grupoproced, grupoprocedimento.cod_grupoproced, agendamento.agendamento_id, agendamento.";
            sql += "status, agendamento.cod_plano, agendamento.cod_convenio, convenio.descr_convenio,";
            sql += "paciente.ddd_residencial, paciente.tel_residencial, agendamento.agendamento_id, ";
            sql += "paciente.ddd_celular, paciente.tel_celular, agendamento.encaixe, agendamento.retorno, ";
            sql += "paciente.ddd_comercial, paciente.tel_comercial, grupoprocedimento.cor ";
            sql += "FROM grupoprocedimento ";
            sql += "INNER JOIN ((paciente INNER JOIN agendamento ON ";
            sql += "paciente.codcli = agendamento.codcli) INNER JOIN ";
            sql += "convenio ON agendamento.cod_convenio = convenio.cod_convenio)";
            sql += " ON grupoprocedimento.cod_grupoproced = agendamento.";
            sql += "cod_proced ";
            sql += "WHERE agendamento.ativo='S' AND agendamento.data='" + formataDataBusca(data) + "' ";
            sql += "AND agendamento.prof_reg='" + prof_reg + "' ";
            if (encaixe == 0) {
                sql += "AND hora >='" + toString(data) + "' ";
                sql += "AND hora <'" + toString(Util.addMinutos(data, intervalo)) + "' ";
            } else if (encaixe == -1) {
                sql += "AND hora < '" + toString(data) + "' ";
            } else {
                sql += "AND hora >='" + toString(data) + "' ";
            }
            sql += " ORDER BY hora";
            rs = stmt.executeQuery(sql);
            String agendar = "";
            String fones = "";
            boolean achou = false;
            String cor = "", corproced = "";
            while (rs.next()) {
                eh_encaixe = Util.trataNulo(rs.getString("encaixe"), "N");
                eh_retorno = Util.trataNulo(rs.getString("retorno"), "N");
                corproced = rs.getString("cor");
                fones = "<div style=background-color:#FF9900 class=texto><b>..:Telefones</b></div>Res: (";
                fones += rs.getString("ddd_residencial") != null ? rs.getString("ddd_residencial") : " ";
                fones += ") ";
                fones += rs.getString("tel_residencial") != null ? rs.getString("tel_residencial") : "";
                fones += "<br>Cel: (";
                fones += rs.getString("ddd_celular") != null ? rs.getString("ddd_celular") : " ";
                fones += ") ";
                fones += rs.getString("tel_celular") != null ? rs.getString("tel_celular") : " ";
                fones += "<br>Com: (";
                fones += rs.getString("ddd_comercial") != null ? rs.getString("ddd_comercial") : " ";
                fones += ") ";
                fones += rs.getString("tel_comercial") != null ? rs.getString("tel_comercial") : " ";
                resp = " <a onMouseover=\"ddrivetip('" + fones + "<br>" + "<div style=background-color:#FF9900 class=texto><b>..:Agendas Anteriores</b></div>" + getAgendasAnteriores(rs.getString("codcli"), formataDataBusca(data), prof_reg) + "', 250)\" onMouseout=\"hideddrivetip()\" href=\"JavaScript:irCadastro(" + rs.getString("codcli") + ")\"'>" + rs.getString("nome") + "</a> ( " + rs.getString("grupoproced") + " - " + rs.getString("convenio.descr_convenio") + " )";
                agendar = "<a href=\"Javascript:excluiragenda(" + rs.getString("agendamento_id") + ",'" + prof_reg + "')\" title='Excluir Agendamento'><img src='images/delete.gif' style='width:12px; height:13px; border: 1px solid #000000'></a>";
                agendar += " <a id='status" + rs.getString("agendamento_id") + "' title='Alterar Status' href='Javascript:alteraStatus(" + rs.getString("agendamento_id") + ")'><img id='img" + rs.getString("agendamento_id") + "' src='images/" + rs.getString("status") + ".gif' style='width:13px; height:13px; border: 1px solid #000000'></a>";
                agendar += " <a title='Observa��es' href='Javascript: verObs(" + rs.getString("agendamento_id") + "," + rs.getString("codcli") + ")'><img src='images/obs.gif' style='width:13px; height:13px; border: 1px solid #000000'></a>";
                if (this.fezLancamentoFinanceiro(rs.getString("agendamento_id"))) agendar += " <a title='Lan�amento Executado' href=\"Javascript:alert('Lan�amento j� efetuado')\"><img src='images/moeda2.gif' style='width:13px; height:13px; border: 1px solid #000000'></a>"; else agendar += " <a title='Lan�ar Atendimento' href=\"Javascript: lancarProcedimento('" + formataData(data) + "','" + toString(data) + "','" + rs.getString("cod_grupoproced") + "','" + rs.getString("paciente.nome") + "','" + rs.getString("codcli") + "','" + prof_reg + "','" + rs.getString("cod_convenio") + "','" + rs.getString("cod_plano") + "'," + rs.getString("agendamento_id") + ")\"><img id='moeda" + rs.getString("agendamento_id") + "' src='images/moeda.gif' style='width:13px; height:13px; border: 1px solid #000000'></a>";
                agendar += " <a title='Reagendar Paciente' href=\"Javascript: reagendar(" + rs.getString("agendamento_id") + ")\"><img src='images/27.gif' style='width:13px; height:13px; border: 1px solid #000000'></a>";
                if (toString(data).equals(Util.formataHora(rs.getString("hora")))) {
                    achou = true;
                }
                estrutura += "<tr>";
                estrutura += " <td width='40' class='tdLight' style='background-color:" + corproced + "'>" + Util.formataHora(rs.getString("hora")) + "</td>\n";
                if (eh_retorno.equals("S")) {
                    cor = "#FFFF99";
                } else if (Util.primeiraVez(rs.getString("codcli"), formataData(data), true)) {
                    cor = "#CEFFCE";
                } else {
                    cor = "white";
                }
                if (eh_encaixe.equals("S")) {
                    estrutura += " <td class='tdLight' style='background-color:" + cor + "; heigth:20px'><b>" + agendar + resp + "</b></td>\n";
                } else {
                    estrutura += " <td class='tdLight' style='background-color:" + cor + "; heigth:20px'>" + agendar + resp + "</td>\n";
                }
                estrutura += "</tr>";
            }
            if (!achou && encaixe == 0) {
                String estruturaaux = "";
                String cancelaAgenda[] = existeCancelamentoAgenda(data, prof_reg);
                if (!Util.isNull(cancelaAgenda[1])) {
                    estruturaaux += "<tr>";
                    estruturaaux += " <td width='40' class='tdLight'>" + toString(data) + "</td>\n";
                    if (cancelaAgenda[0].equalsIgnoreCase("almoco")) estruturaaux += " <td class='tdLight' style='background-color:#FFFFFF; heigth:20px'><a href=\"Javascript:alert('N�o � poss�vel desbloquear hor�rio de almo�o.\\nSe precisar agendar, use o encaixe.')\" title='Desbloquear Hor�rio'><img src='images/cadeado.gif' heigth='13' border='0'></a> " + cancelaAgenda[1] + "</td>\n"; else estruturaaux += " <td class='tdLight' style='background-color:#FFFFFF; heigth:20px'><a href='Javascript:desbloquearagenda(" + cancelaAgenda[0] + ")' title='Desbloquear Hor�rio'><img src='images/cadeado.gif' heigth='13' border='0'></a> " + cancelaAgenda[1] + "</td>\n";
                    estruturaaux += "</tr>";
                } else {
                    agendar = "<a href=\"Javascript:agendar('" + prof_reg + "','" + toString(data) + "')\" title='Agendar Paciente'><img src='images/agenda.gif' border=0 heigth='13'></a> ";
                    agendar += " <a href=\"Javascript:bloquearagenda('" + prof_reg + "','" + toString(data) + "')\" title='Bloquear Hor�rio'><img src='images/cadeadoaberto.gif' heigth='13' border=0></a> ";
                    estruturaaux = "<tr>";
                    estruturaaux += " <td width='40' class='tdLight'>" + toString(data) + "</td>\n";
                    estruturaaux += " <td class='tdLight' style='background-color:white; heigth:20px'>" + agendar + "</td>\n";
                    estruturaaux += "</tr>";
                }
                estrutura = estruturaaux + estrutura;
            }
            rs.close();
            stmt.close();
            return estrutura;
        } catch (SQLException e) {
            return e.toString();
        }
    }

    private boolean fezLancamentoFinanceiro(String agenda_id) {
        boolean resp = false;
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            String sql = "";
            ResultSet rs = null;
            sql = "SELECT faturas.Numero ";
            sql += "FROM (agendamento INNER JOIN (faturas_itens INNER JOIN faturas ON ";
            sql += "faturas_itens.Numero = faturas.Numero) ON (faturas.Data_Lanca = agendamento.data) ";
            sql += "AND (agendamento.codcli = faturas.codcli)) INNER JOIN procedimentos ON ";
            sql += "(agendamento.cod_proced = procedimentos.grupoproced) AND ";
            sql += "(faturas_itens.Cod_Proced = procedimentos.COD_PROCED) ";
            sql += "WHERE agendamento.agendamento_id=" + agenda_id;
            rs = stmt.executeQuery(sql);
            if (rs.next()) resp = true; else resp = false;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resp;
    }

    private String formataDataBusca(GregorianCalendar data) {
        String resp = "";
        int d, m, a;
        d = data.get(Calendar.DATE);
        m = data.get(Calendar.MONTH) + 1;
        a = data.get(Calendar.YEAR);
        resp = a + "-" + m + "-" + d;
        return resp;
    }

    private String formataData(GregorianCalendar data) {
        String resp = "";
        int d, m, a;
        d = data.get(Calendar.DATE);
        m = data.get(Calendar.MONTH) + 1;
        a = data.get(Calendar.YEAR);
        String sd = d < 10 ? "0" + d : "" + d;
        String sm = m < 10 ? "0" + m : "" + m;
        String sa = "" + a;
        resp = sd + "/" + sm + "/" + sa;
        return resp;
    }

    public String montaAgendaMedicoFrame(String prof_reg) {
        String sql = "";
        String resp = "<table width='100%' cellpadding='0' cellspacing='0' width='100%'>";
        try {
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            sql = "SELECT paciente.nome, agendamento.hora, paciente.codcli, agendamento.status ";
            sql += "FROM agendamento INNER JOIN paciente ";
            sql += "ON agendamento.codcli = paciente.codcli ";
            sql += "WHERE agendamento.ativo='S' AND agendamento.prof_reg='" + prof_reg + "' AND ";
            sql += "agendamento.data='" + Util.formataDataInvertida(Util.getData()) + "' ";
            sql += "AND agendamento.status <> 9 ";
            sql += "ORDER BY hora";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                resp += "<tr>\n";
                resp += " <td class='texto' width='100%' style='color:black; font-size:8px'>";
                resp += " <a title='Atender Paciente' href=\"historicopac.jsp?codcli=" + rs.getString("codcli") + "\" target='mainFrame'>";
                if (rs.getString("status").equals("3")) resp += "<font color='red'>"; else resp += "<font color='blue'>";
                resp += Util.formataHora(rs.getString("hora"));
                resp += "</font>-" + rs.getString("nome") + "</a></td>\n";
                resp += "</tr>\n";
            }
            resp += "</table>";
            rs.close();
            stmt.close();
            return resp;
        } catch (Exception e) {
            return "ERRO: " + e.toString() + "SQL: " + sql;
        }
    }

    public String montaAgendaMedico(int dia, int mes, int ano, String prof_reg, String cod_empresa, String usuario_logado) {
        String sql = "";
        String resp = "<table width='100%' cellpadding='0' cellspacing='0'>";
        GregorianCalendar gc_inicio = null, gc_fim = null;
        String Strinicio = "00:00", Strfim = "00:00";
        int intervalo_consulta = 0;
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            sql = "SELECT tempoconsulta FROM profissional WHERE prof_reg='" + prof_reg + "'";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                intervalo_consulta = rs.getInt("tempoconsulta");
            } else {
                intervalo_consulta = 5;
            }
            gc_inicio = Util.toTime(Strinicio, dia, mes, ano);
            int dia_semana = gc_inicio.get(Calendar.DAY_OF_WEEK);
            String data = Util.formataDataInvertida(Util.formataData(dia, mes, ano));
            sql = "SELECT hora_inicio, hora_fim FROM agendamedico WHERE ";
            sql += "prof_reg='" + prof_reg + "' AND dia_semana=" + dia_semana + " ";
            sql += "AND agendamedico.vigencia IN (";
            sql += "SELECT MAX(agendamedico.vigencia) ";
            sql += "FROM agendamedico WHERE vigencia <= '" + data;
            sql += "' AND prof_reg='" + prof_reg + "') ";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                Strinicio = rs.getString("hora_inicio");
                Strfim = rs.getString("hora_fim");
            }
            gc_inicio = Util.toTime(Strinicio, dia, mes, ano);
            gc_fim = Util.toTime(Strfim, dia, mes, ano);
            if (Strinicio.equals(Strfim)) {
                return resp;
            }
            resp += getAgendaMedico(gc_inicio, prof_reg, intervalo_consulta, -1, usuario_logado);
            while (Util.emMinutos(gc_inicio) <= Util.emMinutos(Util.addMinutos(gc_fim, -intervalo_consulta))) {
                resp += getAgendaMedico(gc_inicio, prof_reg, intervalo_consulta, 0, usuario_logado);
                gc_inicio = Util.addMinutos(gc_inicio, intervalo_consulta);
            }
            resp += getAgendaMedico(gc_fim, prof_reg, intervalo_consulta, 1, usuario_logado);
            resp += "</table>";
            rs.close();
            stmt.close();
            return resp;
        } catch (Exception e) {
            return e.toString();
        }
    }

    public String getAgendaMedico(GregorianCalendar data, String prof_reg, int intervalo, int encaixe) {
        return getAgendaMedico(data, prof_reg, intervalo, encaixe, "");
    }

    public String getAgendaMedico(GregorianCalendar data, String prof_reg, int intervalo, int encaixe, String usuario_logado) {
        String sql = "";
        String resp = "";
        String cor = "", negrito = "";
        ResultSet rs = null;
        boolean achou = false;
        String eh_encaixe = "";
        String eh_retorno = "";
        try {
            sql = "SELECT paciente.nome, paciente.codcli, paciente.";
            sql += "data_nascimento, grupoprocedimento.grupoproced, ";
            sql += "agendamento.status, agendamento.hora, agendamento.obs, convenio.";
            sql += "descr_convenio, agendamento.cod_convenio, agendamento.encaixe, agendamento.retorno ";
            sql += "FROM (grupoprocedimento INNER JOIN ";
            sql += "(paciente INNER JOIN agendamento ON paciente.codcli ";
            sql += "= agendamento.codcli) ";
            sql += "ON grupoprocedimento.cod_grupoproced = agendamento.";
            sql += "cod_proced) LEFT JOIN convenio ON ";
            sql += "agendamento.cod_convenio = convenio.cod_convenio ";
            sql += "WHERE agendamento.ativo='S' AND agendamento.prof_reg='" + prof_reg + "' AND ";
            sql += "agendamento.data='" + formataDataBusca(data) + "' ";
            if (encaixe == 0) {
                sql += "AND hora >='" + toString(data) + "' AND hora <'";
                sql += toString(Util.addMinutos(data, intervalo)) + "'";
            } else if (encaixe == -1) {
                sql += "AND hora < '" + toString(data) + "' ";
            } else {
                sql += "AND hora >='" + toString(data) + "' ";
            }
            sql += " ORDER BY hora";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                eh_encaixe = Util.trataNulo(rs.getString("encaixe"), "N");
                eh_retorno = Util.trataNulo(rs.getString("retorno"), "N");
                if (eh_retorno.equals("S")) {
                    cor = "#FFFF99";
                } else if (Util.primeiraVez(rs.getString("codcli"), formataData(data), true)) {
                    cor = "#CEFFCE";
                } else {
                    cor = "white";
                }
                if (eh_encaixe.equals("S")) {
                    negrito = "font-weight: bold";
                } else {
                    negrito = "";
                }
                String nomeconvenio = rs.getString("descr_convenio") != null ? rs.getString("descr_convenio") : "Sem conv�nio";
                resp += "<tr>\n";
                resp += "  <td class='tdMedium' width='80px' style='background-color:" + cor + ";" + negrito + "'>" + Util.formataHora(rs.getString("hora")) + "</td>\n";
                resp += "  <td class='tdLight' width='100%' style='background-color:" + cor + ";" + negrito + "'><a onMouseover=\"ddrivetip('" + getAgendasAnteriores(rs.getString("codcli"), formataDataBusca(data), prof_reg) + "', 350)\" onMouseout=\"hideddrivetip()\" href=\"Javascript:verHistoricos('" + rs.getString("codcli") + "','" + rs.getString("nome") + "','" + Util.formataData(rs.getString("data_nascimento")) + "','" + rs.getString("cod_convenio") + "','" + rs.getString("descr_convenio") + "')\"><img src='images/" + rs.getString("status") + ".gif' border=0> &nbsp;" + rs.getString("nome") + " ( " + rs.getString("grupoproced") + " - " + nomeconvenio + " ) " + "</a></td>\n";
                resp += "</tr>\n";
                resp += "<tr>\n";
                resp += "  <td class='tdMedium' style='background-color:" + cor + ";" + negrito + "'>OBS:</td>\n";
                resp += "  <td class='tdLight' style='background-color:" + cor + ";" + negrito + "'><pre>" + (rs.getString("obs") != null ? rs.getString("obs") : "&nbsp;") + "</pre>&nbsp;</td>\n";
                resp += "</tr>\n";
                resp += "<tr>\n";
                resp += "  <td colspan=2 height='10px'></td>\n";
                resp += "</tr>\n";
                achou = true;
            }
            if (!Util.isNull(usuario_logado)) {
                resp += this.buscaAgendaPessoal(data, prof_reg, intervalo, usuario_logado, encaixe);
            }
            if (!achou && encaixe == 0) {
                String cancelaAgenda[] = existeCancelamentoAgenda(data, prof_reg);
                if (!Util.isNull(cancelaAgenda[1])) {
                    resp += "<tr>\n";
                    resp += "  <td class='tdMedium' width='80px' style='background-color:white'>" + toString(data) + "</td>\n";
                    resp += "  <td class='tdLight' width='100%' style='background-color:white'>" + cancelaAgenda[1] + "</td>\n";
                    resp += "</tr>\n";
                } else {
                    resp += "<tr>\n";
                    resp += "  <td class='tdMedium' width='80px' style='background-color:white'>" + toString(data) + "</td>\n";
                    resp += "  <td class='tdLight' width='100%' style='background-color:white'>&nbsp;</td>\n";
                    resp += "</tr>\n";
                }
                resp += "<tr>\n";
                resp += "  <td class='tdMedium' style='background-color:white'>OBS:</td>\n";
                resp += "  <td class='tdLight' style='background-color:white'>&nbsp;</td>\n";
                resp += "</tr>\n";
                resp += "<tr>\n";
                resp += "  <td colspan=2 height='10px'></td>\n";
                resp += "</tr>\n";
            }
            return resp;
        } catch (SQLException ex) {
            return "ERRO: " + ex.toString() + " SQL: " + sql;
        }
    }

    public String buscaAgendaPessoal(GregorianCalendar data, String prof_reg, int intervalo, String usuario_logado, int encaixe) {
        String resp = "", sql = "";
        try {
            Statement stmt2 = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            sql += "SELECT agendapessoal.hora, agendapessoal.descricao ";
            sql += "FROM t_usuario INNER JOIN agendapessoal ON ";
            sql += "t_usuario.cd_usuario = agendapessoal.cd_usuario ";
            sql += "WHERE t_usuario.prof_reg='" + prof_reg + "' AND t_usuario.cd_usuario=" + usuario_logado;
            sql += " AND data='" + formataDataBusca(data) + "' ";
            if (encaixe == 0) {
                sql += "AND hora >='" + toString(data) + "' AND hora <'";
                sql += toString(Util.addMinutos(data, intervalo)) + "'";
            } else if (encaixe == -1) {
                sql += "AND hora < '" + toString(data) + "' ";
            } else {
                sql += "AND hora >='" + toString(data) + "' ";
            }
            ResultSet rs = null;
            rs = stmt2.executeQuery(sql);
            while (rs.next()) {
                resp += "<tr>\n";
                resp += "  <td class='tdMedium' width='80px' style='background-color:white'>" + Util.formataHora(rs.getString("hora")) + "</td>\n";
                resp += "  <td class='tdLight' width='100%' style='background-color:white'><img src='images/17.gif'>&nbsp; " + rs.getString("descricao") + "</td>\n";
                resp += "</tr>\n";
                resp += "<tr>\n";
                resp += "  <td class='tdMedium' style='background-color:white'>OBS:</td>\n";
                resp += "  <td class='tdLight' style='background-color:white'>&nbsp;</td>\n";
                resp += "</tr>\n";
                resp += "<tr>\n";
                resp += "  <td colspan=2 height='10px'></td>\n";
                resp += "</tr>\n";
            }
            rs.close();
            stmt2.close();
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL: " + sql;
        }
        return resp;
    }

    public String[] existeCancelamentoAgenda(GregorianCalendar data, String prof_reg) {
        String resp[] = { "", "" }, sql = "";
        String hora = toString(data);
        try {
            Statement stmt2 = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            sql += "SELECT cod_feriado, descricao FROM feriados WHERE ";
            sql += "(prof_reg='" + prof_reg + "' AND dataInicio<='" + formataDataBusca(data) + "' AND dataFim>='" + formataDataBusca(data) + "' AND definitivo='N' AND diatodo='S') OR ";
            sql += "(prof_reg='" + prof_reg + "' AND dataInicio<='" + formataDataBusca(data) + "' AND dataFim>='" + formataDataBusca(data) + "' AND hora_inicio <= '" + hora + "' AND hora_fim >= '" + hora + "' AND definitivo='N' AND diatodo='N') OR ";
            sql += "(prof_reg='" + prof_reg + "' AND DAY(datainicio)=" + data.get(Calendar.DATE) + " AND MONTH(datainicio)=" + (data.get(Calendar.MONTH) + 1) + " AND definitivo='S' AND diatodo='S') OR ";
            sql += "(prof_reg='" + prof_reg + "' AND DAY(datainicio)=" + data.get(Calendar.DATE) + " AND MONTH(datainicio)=" + (data.get(Calendar.MONTH) + 1) + " AND hora_inicio <= '" + hora + "' AND hora_fim >= '" + hora + "' AND definitivo='S' AND diatodo='N') OR ";
            sql += "(prof_reg='todos' AND DAY(datainicio)=" + data.get(Calendar.DATE) + " AND MONTH(datainicio)=" + (data.get(Calendar.MONTH) + 1) + " AND hora_inicio <= '" + hora + "' AND hora_fim >= '" + hora + "' AND definitivo='S' AND diatodo='N') OR ";
            sql += "(prof_reg='todos' AND dataInicio<='" + formataDataBusca(data) + "' AND dataFim>='" + formataDataBusca(data) + "' AND hora_inicio <= '" + hora + "' AND hora_fim >= '" + hora + "' AND definitivo='N' AND diatodo='N')";
            ResultSet rs = null;
            rs = stmt2.executeQuery(sql);
            if (rs.next()) {
                resp[0] = rs.getString("cod_feriado");
                resp[1] = "<font color='red'>" + rs.getString("descricao") + "</font>";
            } else {
                int dia_semana = data.get(Calendar.DAY_OF_WEEK);
                sql = "SELECT * FROM agendamedico WHERE dia_semana=" + dia_semana;
                sql += " AND inicio_almoco <= '" + hora + "' AND fim_almoco >'" + hora + "' ";
                sql += "AND prof_reg='" + prof_reg + "' ";
                sql += "AND agendamedico.vigencia IN (";
                sql += "SELECT MAX(agendamedico.vigencia) ";
                sql += "FROM agendamedico WHERE vigencia <= '" + formataDataBusca(data);
                sql += "' AND prof_reg='" + prof_reg + "') ";
                rs = stmt2.executeQuery(sql);
                if (rs.next()) {
                    resp[0] = "almoco";
                    resp[1] = "<font color='red'>Hor�rio de Almo�o</font>";
                }
            }
            rs.close();
            stmt2.close();
        } catch (SQLException e) {
            resp[1] = "ERRO: " + e.toString() + " SQL: " + sql;
        }
        return resp;
    }

    public String[] getAgenda(String id) {
        String sql = "";
        String resp[] = { "", "", "", "", "", "", "", "", "" };
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            sql += "SELECT paciente.nome, paciente.email, paciente.ddd_celular, ";
            sql += "paciente.tel_celular, profissional.nome, ";
            sql += "profissional.email, grupoprocedimento.grupoproced, ";
            sql += "agendamento.data, agendamento.hora ";
            sql += "FROM grupoprocedimento INNER JOIN ((paciente INNER ";
            sql += "JOIN agendamento ON paciente.codcli = ";
            sql += "agendamento.codcli) INNER JOIN profissional ";
            sql += "ON agendamento.prof_reg = profissional.prof_reg";
            sql += ") ON grupoprocedimento.cod_grupoproced = agendamento.cod_proced ";
            sql += "WHERE agendamento.agendamento_id=" + id;
            ResultSet rs = null;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                resp[0] = rs.getString("paciente.nome");
                resp[1] = rs.getString("profissional.nome");
                resp[2] = rs.getString("profissional.email");
                resp[3] = rs.getString("grupoproced");
                resp[4] = rs.getString("data");
                resp[5] = rs.getString("hora");
                resp[6] = rs.getString("paciente.email");
                resp[7] = rs.getString("paciente.ddd_celular");
                resp[8] = rs.getString("paciente.tel_celular");
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp[0] = e.toString();
            return resp;
        }
    }

    public String[] pegaConsultas(String codcli, String data, String cod_proced) {
        String resp[] = { "", "" };
        Statement stmt = null;
        String sql = "";
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            sql = "SELECT agendamento.data FROM agendamento INNER JOIN ";
            sql += "(procedimentos INNER JOIN grupoprocedimento ON ";
            sql += "procedimentos.grupoproced = grupoprocedimento.cod_grupoproced) ";
            sql += "ON agendamento.cod_proced = grupoprocedimento.cod_grupoproced ";
            sql += "WHERE agendamento.ativo='S' AND procedimentos.tipoGuia=1 AND ";
            sql += "codcli=" + codcli + " AND agendamento.data < '";
            sql += Util.formataDataInvertida(data) + "' ";
            sql += "AND agendamento.cod_proced='" + cod_proced + "' ";
            sql += "AND agendamento.status<>3 ";
            sql += "AND retorno='N' ";
            sql += "ORDER BY data DESC LIMIT 1";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                resp[0] = Util.formataData(rs.getString("data"));
            } else {
                resp[0] = "Primeira vez para esse procedimento";
            }
            sql = "SELECT agendamento.data FROM agendamento INNER JOIN ";
            sql += "(procedimentos INNER JOIN grupoprocedimento ON ";
            sql += "procedimentos.grupoproced = grupoprocedimento.cod_grupoproced) ";
            sql += "ON agendamento.cod_proced = grupoprocedimento.cod_grupoproced ";
            sql += "WHERE agendamento.ativo='S' AND procedimentos.tipoGuia=1 AND ";
            sql += "codcli=" + codcli + " AND agendamento.data > '";
            sql += Util.formataDataInvertida(data) + "' ";
            sql += "AND agendamento.cod_proced='" + cod_proced + "' ORDER BY data ASC LIMIT 1";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                resp[1] = Util.formataData(rs.getString("data"));
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp[0] = "ERRO: " + e.toString() + " SQL: " + sql;
            return resp;
        }
    }

    public String[] proxData(String codcli, String cod_proced, String cod_plano) {
        String resp[] = { "", "" };
        Statement stmt = null;
        String sql = "";
        if (Util.isNull(codcli)) {
            resp[0] = "Sem paciente";
            resp[1] = Util.getData();
            return resp;
        }
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            sql = "SELECT agendamento.data FROM agendamento INNER JOIN ";
            sql += "(procedimentos INNER JOIN grupoprocedimento ON ";
            sql += "procedimentos.grupoproced = grupoprocedimento.cod_grupoproced) ";
            sql += "ON agendamento.cod_proced = grupoprocedimento.cod_grupoproced ";
            sql += "WHERE agendamento.ativo='S' AND procedimentos.tipoGuia=1 AND ";
            sql += "codcli=" + codcli + " AND agendamento.data <= '";
            sql += Util.formataDataInvertida(Util.getData()) + "' ";
            sql += "AND agendamento.cod_proced='" + cod_proced + "' ";
            sql += "AND agendamento.status<>3 ";
            sql += "AND retorno='N' ";
            sql += "ORDER BY data DESC LIMIT 1";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                resp[0] = Util.formataData(rs.getString("data"));
            } else {
                resp[0] = "Primeira vez";
            }
            sql = "SELECT retorno_consulta FROM convenio WHERE cod_convenio=" + cod_plano.split("@")[0];
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                int dias = rs.getInt("retorno_consulta");
                if (resp[0].length() > 10) resp[1] = Util.getData(); else {
                    String datanova = Util.addDias(resp[0], dias + 2);
                    if (Util.getDifDate(datanova, Util.getData()) < 0) resp[1] = datanova; else resp[1] = Util.getData();
                }
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp[0] = "ERRO: " + e.toString() + " SQL: " + sql;
            return resp;
        }
    }

    public String getObs(String cod_agenda) {
        String sql = "";
        String resp = "";
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            sql += "SELECT obs FROM agendamento WHERE agendamento_id=" + cod_agenda;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                resp = rs.getString("obs");
                if (resp == null) {
                    resp = "";
                }
            } else {
                resp = "";
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = e.toString() + sql;
            return resp;
        }
    }

    public String getAgendasAnteriores(String codcli, String data, String prof_reg) {
        String sql = "";
        String resp = "";
        boolean achou = false;
        try {
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            sql = "SELECT status, data, hora, agendamento.prof_reg AS prof_reg, nome FROM agendamento ";
            sql += "INNER JOIN profissional ON (";
            sql += "agendamento.prof_reg=profissional.prof_reg) ";
            sql += "WHERE agendamento.ativo='S' AND codcli=" + codcli;
            sql += " AND data < '" + data + "' ORDER BY data DESC LIMIT 5";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                resp += "<img src=images/" + rs.getInt("status") + ".gif> ";
                resp += Util.formataData(rs.getString("data")) + " �s " + Util.formataHora(rs.getString("hora"));
                if (!prof_reg.equals(rs.getString("prof_reg"))) {
                    resp += " (" + rs.getString("nome") + ")";
                }
                resp += "<br>";
                achou = true;
            }
            if (!achou) {
                resp += "Primeira agenda do paciente";
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = e.toString() + sql;
            return resp;
        }
    }

    public String setObs(String cod_agenda, String obs) {
        String sql = "UPDATE agendamento set obs='" + obs + "' WHERE agendamento_id=" + cod_agenda;
        return new Banco().executaSQL(sql);
    }

    public String excluirAgenda(String cod_agenda, String cod_usuario) {
        String sql = "";
        sql += "UPDATE agendamento set ativo='N', usuario_exclusao = " + cod_usuario;
        sql += ", data_exclusao='" + Util.formataDataInvertida(Util.getData());
        sql += "', hora_exclusao='" + Util.getHora();
        sql += "' WHERE agendamento_id=" + cod_agenda;
        return new Banco().executaSQL(sql);
    }

    public String getProcedimentos(String cod_plano, String cod_grupoproced) {
        String sql = "";
        String resp = "";
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            boolean entrou = false;
            ResultSet rs = null;
            sql = "SELECT procedimentos.COD_PROCED, procedimentos.Procedimento, ";
            sql += "valorprocedimentos.valor ";
            sql += "FROM procedimentos INNER JOIN valorprocedimentos ON ";
            sql += "procedimentos.COD_PROCED = valorprocedimentos.cod_proced ";
            sql += "WHERE valorprocedimentos.cod_plano=" + cod_plano;
            sql += " AND procedimentos.grupoproced=" + cod_grupoproced;
            rs = stmt.executeQuery(sql);
            resp += "<table cellspacing=0 cellpadding=0 width='100%'>\n";
            while (rs.next()) {
                resp += "<tr onClick=\"escolheProcedimento('" + rs.getString("COD_PROCED") + "','" + rs.getString("Procedimento") + "','" + rs.getString("valor") + "')\" onMouseOver='trocaCor(this,1);' onMouseOut='trocaCor(this,2);'>";
                resp += "<td class='tdLight'>";
                resp += rs.getString("Procedimento");
                resp += "</td></tr>\n";
                entrou = true;
            }
            if (!entrou) {
                resp += "<tr>";
                resp += "  <td class='tdMedium' width='100%'>Dados Incompletos</td>";
                resp += "</tr>";
                resp += "<tr>";
                resp += "  <td class='tdLight'>Falta informa��es para o lan�amento. Valor do procedimento ou v�nculo ao grupo est� faltando</td>";
                resp += "</tr>";
            }
            resp += "</table>";
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL: " + sql;
            return resp;
        }
    }

    public String[] getDadosConvenio(String cod_convenio) {
        String sql = "";
        String resp[] = { "", "" };
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            sql = "SELECT convenio.retorno_consulta, convenio.descr_convenio ";
            sql += "FROM convenio WHERE cod_convenio=" + cod_convenio;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                resp[0] = rs.getString("descr_convenio");
                resp[1] = rs.getString("retorno_consulta");
            } else {
                resp[0] = "N�o encontrado!";
                resp[1] = "N/C";
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp[0] = "ERRO: " + e.toString() + " SQL: " + sql;
            return resp;
        }
    }

    public int[] navegaData(int dia, int mes, int ano, int qtde) {
        int resp[] = null;
        try {
            resp = new int[3];
            GregorianCalendar data = new GregorianCalendar(ano, mes - 1, dia);
            data.add(GregorianCalendar.DATE, qtde);
            resp[0] = data.get(Calendar.DATE);
            resp[1] = data.get(Calendar.MONTH) + 1;
            resp[2] = data.get(Calendar.YEAR);
        } catch (Exception e) {
        }
        return resp;
    }

    public String getProximasAgendas(String[] diassemana, String data, String prof_reg, String manha, String tarde, String cod_proced, String cod_empresa) {
        String sql = "";
        String resp = "";
        String prof_reg_local = "";
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            GregorianCalendar inicio = Util.toTime("00:00:00", Integer.parseInt(Util.getDia(data)), Integer.parseInt(Util.getMes(data)), Integer.parseInt(Util.getAno(data)));
            int dia_semana = inicio.get(Calendar.DAY_OF_WEEK);
            sql = "SELECT prof_reg, nome, LEFT(profissional.nome,15) AS nome15, tempoconsulta ";
            sql += "FROM profissional ";
            sql += "WHERE profissional.ativo = 'S' AND profissional.exibir = 'S' AND locacao='interno' ";
            sql += "AND cod_empresa=" + cod_empresa;
            if (!Util.isNull(prof_reg) && !prof_reg.equals("todos")) {
                sql += " AND profissional.prof_reg='" + prof_reg + "' ";
            }
            sql += " ORDER BY nome";
            rs = stmt.executeQuery(sql);
            resp = "<div style='width:380px;height:38px; overflow: auto'>\n";
            resp += "<table cellspacing=0 cellpadding=0 class='table' width='98%'>\n<tr>\n";
            while (rs.next()) {
                resp += "<td id='cabecalho" + rs.getString("profissional.prof_reg") + "' class='tdMedium' style='padding:0px'>&nbsp; <a title='" + rs.getString("nome") + "' href=\"Javascript:escolheProfissional('" + rs.getString("prof_reg") + "')\">" + rs.getString("nome15") + "...</a></td>\n";
            }
            resp += "</tr>\n</table>\n</div>\n";
            rs.beforeFirst();
            while (rs.next()) {
                prof_reg_local = rs.getString("prof_reg");
                resp += "<div id='medico" + prof_reg_local + "' style='display:none'>\n";
                resp += "<table cellspacing='0' cellpadding='0' border='0' class='table'>\n";
                resp += getAgendaLivreMedico(diassemana, data, prof_reg_local, manha, tarde, cod_proced, rs.getInt("tempoconsulta"), 0, 0);
                resp += "</table>\n</div>\n";
            }
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL: " + sql;
        }
        return resp;
    }

    public String getAgendaLivreMedico(String[] diassemana, String data, String prof_reg, String manha, String tarde, String cod_proced, int intervalo, int cont, int tentativas) {
        String sql = "";
        String resp = "";
        int dia, mes, ano;
        int totaldehorarioslivres = 10;
        int totaltentativas = 100;
        GregorianCalendar gc_iniciomedico, gc_fimmedico;
        String strinicio, strfim;
        try {
            dia = Integer.parseInt(Util.getDia(data));
            mes = Integer.parseInt(Util.getMes(data));
            ano = Integer.parseInt(Util.getAno(data));
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            GregorianCalendar inicio = Util.toTime("00:00:00", Integer.parseInt(Util.getDia(data)), Integer.parseInt(Util.getMes(data)), Integer.parseInt(Util.getAno(data)));
            int dia_semana = inicio.get(Calendar.DAY_OF_WEEK);
            sql = "SELECT agendamedico.hora_inicio, agendamedico.hora_fim ";
            sql += "FROM agenda_procedimento INNER JOIN agendamedico ON ";
            sql += "agenda_procedimento.agenda_id = agendamedico.agenda_id ";
            sql += "WHERE agendamedico.dia_semana=" + dia_semana;
            sql += " AND prof_reg='" + prof_reg + "' ";
            sql += "AND agendamedico.vigencia IN (";
            sql += "SELECT MAX(agendamedico.vigencia) ";
            sql += "FROM agendamedico WHERE vigencia <= '" + Util.formataDataInvertida(data);
            sql += "' AND prof_reg='" + prof_reg + "') ";
            if (!Util.isNull(cod_proced)) {
                sql += " AND agenda_procedimento.cod_proced=" + cod_proced;
            }
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                strinicio = rs.getString("hora_inicio");
                strfim = rs.getString("hora_fim");
                gc_iniciomedico = Util.toTime(strinicio, dia, mes, ano);
                gc_fimmedico = Util.toTime(strfim, dia, mes, ano);
                resp = "";
                boolean processar = false;
                while (Util.emMinutos(gc_iniciomedico) < Util.emMinutos(gc_fimmedico) && cont < totaldehorarioslivres) {
                    String ag = "";
                    processar = true;
                    if (Util.isNull(manha)) {
                        if (Util.emMinutos(gc_iniciomedico) < Util.emMinutos(Util.toTime("12:00", 1, 1, 1))) {
                            processar = false;
                        }
                    }
                    if (Util.isNull(tarde)) {
                        if (Util.emMinutos(gc_iniciomedico) > Util.emMinutos(Util.toTime("12:00", 1, 1, 1))) {
                            processar = false;
                        }
                    }
                    if (Util.getData().equals(formataData(gc_iniciomedico)) && Util.emMinutos(gc_iniciomedico) < Util.emMinutos(Util.toTime(Util.getHora(), 1, 1, 1))) {
                        processar = false;
                    }
                    if (processar) {
                        if (new Banco().pertence(dia_semana + "", diassemana)) {
                            ag = getAgendaLivre(gc_iniciomedico, prof_reg);
                        }
                        if (!ag.equals("")) {
                            resp += ag;
                            cont++;
                        }
                    }
                    gc_iniciomedico = Util.addMinutos(gc_iniciomedico, intervalo);
                }
            }
            if (cont < totaldehorarioslivres && tentativas < totaltentativas) {
                GregorianCalendar proxdia = Util.addMinutos(inicio, 24 * 60);
                String strproxdia = this.formataData(proxdia);
                resp += getAgendaLivreMedico(diassemana, strproxdia, prof_reg, manha, tarde, cod_proced, intervalo, cont, tentativas + 1);
            }
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL: " + sql;
        }
        return resp;
    }

    public String getAgendaLivre(GregorianCalendar data, String prof_reg) {
        String estrutura = "", estruturaaux = "";
        Statement stmt = null;
        try {
            String cancelaAgenda[] = existeCancelamentoAgenda(data, prof_reg);
            String feriado = new AgendadoMedico().ehFeriado(data);
            if (!Util.isNull(cancelaAgenda[1]) || feriado != null) {
                return "";
            }
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            String sql = "";
            ResultSet rs = null;
            sql += "SELECT * FROM agendamento ";
            sql += "WHERE agendamento.data='" + formataDataBusca(data) + "' ";
            sql += "AND agendamento.prof_reg='" + prof_reg + "' ";
            sql += "AND hora ='" + toString(data) + "' ";
            sql += "AND agendamento.ativo='S'";
            rs = stmt.executeQuery(sql);
            String agendar = "";
            if (rs.next()) {
            } else {
                String data1 = formataData(data);
                agendar = "<a href=\"Javascript:agendar(" + Util.getDia(data1) + "," + Util.getMes(data1) + "," + Util.getAno(data1) + ",'" + prof_reg + "','" + toString(data) + "')\" title='Agendar Paciente'><img src='images/agenda.gif' border=0 heigth='13'></a> ";
                estruturaaux = "<tr>";
                estruturaaux += " <td width='100' class='tdLight'>(" + Util.getDiaSemana(data1).substring(0, 3) + ") " + data1 + "</td>\n";
                estruturaaux += " <td width='40' class='tdLight'>" + toString(data) + "</td>\n";
                estruturaaux += " <td class='tdLight' style='background-color:white; heigth:20px'>" + agendar + "</td>\n";
                estruturaaux += "</tr>";
            }
            estrutura = estruturaaux + estrutura;
            rs.close();
            stmt.close();
            return estrutura;
        } catch (SQLException e) {
            return e.toString();
        }
    }

    public String enviaDadosIndiq(String agendamento_id) {
        String sql = "";
        String resp = "";
        String email, nome, nascimento, sexo, cpf, profissional, crm;
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            sql = "SELECT paciente.email, paciente.nome, paciente.data_nascimento, ";
            sql += "paciente.cod_sexo, paciente.cic, profissional.nome, profissional.reg_prof ";
            sql += "FROM (agendamento INNER JOIN paciente ON agendamento.codcli = ";
            sql += "paciente.codcli) INNER JOIN profissional ON ";
            sql += "agendamento.prof_reg = profissional.prof_reg ";
            sql += "WHERE agendamento.agendamento_id=" + agendamento_id;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                email = Util.trataNulo(rs.getString("email"), "");
                nome = Util.trataNulo(rs.getString("paciente.nome"), "");
                nascimento = Util.trataNulo(rs.getString("data_nascimento"), "");
                sexo = Util.trataNulo(rs.getString("cod_sexo"), "");
                cpf = Util.trataNulo(rs.getString("cic"), "");
                cpf = cpf.replace(".", "");
                cpf = cpf.replace("-", "");
                profissional = Util.trataNulo(rs.getString("profissional.nome"), "");
                crm = Util.trataNulo(rs.getString("reg_prof"), "");
                resp = "http://www.indiq.com.br/cad_katu.php?email=" + email + "&nome=" + nome + "&dnasc=" + nascimento;
                resp += "&sexo=" + sexo + "&doc=" + cpf + "&nmedico=" + profissional + "&docm=" + crm;
                resp = resp.replace(" ", "%20");
                if (!Util.isNull(cpf)) {
                    URL url = new URL(resp);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    con.disconnect();
                }
                return "OK";
            } else return "Usu�rio n�o encontrado";
        } catch (SQLException e) {
            return "ERRO no banco de dados: " + e.toString() + " SQL: " + sql;
        } catch (MalformedURLException ex) {
            return "Erro no URL: " + ex.toString();
        } catch (IOException ex) {
            return "Erro de entrada e sa�da: " + ex.toString();
        }
    }

    public static void main(String args[]) {
        Agenda ag = new Agenda();
        String teste;
        teste = ag.montaAgendaMedico(28, 5, 2009, "93080", "92", "");
        System.out.println(teste);
    }
}
