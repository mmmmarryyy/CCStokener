package quebralink;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import sun.misc.BASE64Decoder;

public class QuebraLink {

    private static String validLink = null;

    public String quebraLink(String link) throws StringIndexOutOfBoundsException {
        link = link.replace(".url", "");
        int cod = 0;
        final String linkInit = link.replace("#", "");
        boolean estado = false;
        char letra;
        String linkOrig;
        String newlink = "";
        linkOrig = link.replace("#", "");
        linkOrig = linkOrig.replace(".url", "");
        linkOrig = linkOrig.replace(".html", "");
        linkOrig = linkOrig.replace("http://", "");
        if (linkOrig.contains("clubedodownload")) {
            for (int i = 7; i < linkInit.length(); i++) {
                if (linkOrig.charAt(i) == '/') {
                    for (int j = i + 1; j < linkOrig.length(); j++) {
                        newlink += linkOrig.charAt(j);
                    }
                    if (newlink.contains("//:ptth")) {
                        newlink = inverteFrase(newlink);
                        if (isValid(newlink)) {
                            return newlink;
                        }
                    } else if (newlink.contains("http://")) {
                        if (isValid(newlink)) {
                            return newlink;
                        }
                    }
                }
            }
        }
        if (linkOrig.contains("protetordelink.tv")) {
            for (int i = linkOrig.length() - 1; i >= 0; i--) {
                letra = linkOrig.charAt(i);
                if (letra == '/') {
                    for (int j = i + 1; j < linkOrig.length(); j++) {
                        newlink += linkOrig.charAt(j);
                    }
                    newlink = HexToChar(newlink);
                    if (newlink.contains("ptth")) {
                        if (precisaRepassar(newlink)) {
                            newlink = quebraLink(newlink);
                            newlink = inverteFrase(newlink);
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        } else {
                            newlink = inverteFrase(newlink);
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        }
                    } else {
                        if (precisaRepassar(newlink)) {
                            newlink = quebraLink(newlink);
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        } else {
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        }
                    }
                }
            }
        }
        if (linkOrig.contains("baixeaquifilmes")) {
            for (int i = 0; i < linkOrig.length(); i++) {
                letra = linkOrig.charAt(i);
                if (letra == '?') {
                    for (int j = i + 1; j < linkOrig.length(); j++) {
                        newlink += linkOrig.charAt(j);
                    }
                    if (newlink.contains(":ptth")) {
                        newlink = inverteFrase(newlink);
                        if (precisaRepassar(newlink)) {
                            newlink = quebraLink(newlink);
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        } else {
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        }
                    } else {
                        if (precisaRepassar(newlink)) {
                            newlink = quebraLink(newlink);
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        } else {
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        }
                    }
                }
            }
        }
        if (linkOrig.contains("downloadsgratis")) {
            for (int i = 0; i < linkOrig.length(); i++) {
                letra = linkOrig.charAt(i);
                if (letra == '!') {
                    for (int j = i + 1; j < linkOrig.length(); j++) {
                        newlink += linkOrig.charAt(j);
                    }
                    if (precisaRepassar(QuebraLink.decode64(newlink))) {
                        newlink = quebraLink(QuebraLink.decode64(newlink));
                        if (isValid(newlink)) {
                            return newlink;
                        }
                    } else {
                        if (isValid(newlink)) {
                            return newlink;
                        }
                    }
                }
            }
        }
        newlink = "";
        if (linkOrig.contains("vinxp")) {
            System.out.println("é");
            for (int i = 1; i < linkOrig.length(); i++) {
                if (linkOrig.charAt(i) == '=') {
                    for (int j = i + 1; j < linkOrig.length(); j++) {
                        newlink += linkOrig.charAt(j);
                    }
                    break;
                }
            }
            if (newlink.contains(".vinxp")) {
                newlink = newlink.replace(".vinxp", "");
            }
            newlink = decodeCifraDeCesar(newlink);
            System.out.println(newlink);
            return newlink;
        }
        if (linkOrig.contains("?")) {
            String linkTemporary = "";
            newlink = "";
            if (linkOrig.contains("go!")) {
                linkOrig = linkOrig.replace("?go!", "?");
            }
            if (linkOrig.contains("=")) {
                for (int i = 0; i < linkOrig.length(); i++) {
                    letra = linkOrig.charAt(i);
                    if (letra == '=') {
                        for (int j = i + 1; j < linkOrig.length(); j++) {
                            newlink += linkOrig.charAt(j);
                        }
                        linkTemporary = QuebraLink.decode64(newlink);
                        break;
                    }
                }
                if (linkTemporary.contains("http")) {
                    newlink = "";
                    for (int i = 0; i < linkTemporary.length(); i++) {
                        letra = linkTemporary.charAt(i);
                        if (letra == 'h') {
                            for (int j = i; j < linkTemporary.length(); j++) {
                                newlink += linkTemporary.charAt(j);
                            }
                            newlink = newlink.replace("!og", "");
                            if (precisaRepassar(newlink)) {
                                newlink = quebraLink(newlink);
                                if (isValid(newlink)) {
                                    return newlink;
                                }
                            } else {
                                if (isValid(newlink)) {
                                    return newlink;
                                }
                            }
                        }
                    }
                }
                if (linkTemporary.contains("ptth")) {
                    newlink = "";
                    linkTemporary = inverteFrase(linkTemporary);
                    for (int i = 0; i < linkTemporary.length(); i++) {
                        letra = linkTemporary.charAt(i);
                        if (letra == 'h') {
                            for (int j = i; j < linkTemporary.length(); j++) {
                                newlink += linkTemporary.charAt(j);
                            }
                            newlink = newlink.replace("!og", "");
                            if (precisaRepassar(newlink)) {
                                newlink = quebraLink(newlink);
                                if (isValid(newlink)) {
                                    return newlink;
                                }
                            } else {
                                if (isValid(newlink)) {
                                    return newlink;
                                }
                            }
                        }
                    }
                }
            }
            linkTemporary = "";
            for (int i = 0; i < linkOrig.length(); i++) {
                letra = linkOrig.charAt(i);
                if (letra == '?') {
                    for (int j = i + 1; j < linkOrig.length(); j++) {
                        linkTemporary += linkOrig.charAt(j);
                    }
                    link = QuebraLink.decode64(linkTemporary);
                    break;
                }
            }
            if (link.contains("http")) {
                newlink = "";
                for (int i = 0; i < link.length(); i++) {
                    letra = link.charAt(i);
                    if (letra == 'h') {
                        for (int j = i; j < link.length(); j++) {
                            newlink += link.charAt(j);
                        }
                        newlink = newlink.replace("!og", "");
                        if (precisaRepassar(newlink)) {
                            newlink = quebraLink(newlink);
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        } else {
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        }
                    }
                }
            }
            if (link.contains("ptth")) {
                newlink = "";
                linkTemporary = inverteFrase(link);
                for (int i = 0; i < linkTemporary.length(); i++) {
                    letra = linkTemporary.charAt(i);
                    if (letra == 'h') {
                        for (int j = i; j < linkTemporary.length(); j++) {
                            newlink += linkTemporary.charAt(j);
                        }
                        newlink = newlink.replace("!og", "");
                        if (precisaRepassar(newlink)) {
                            newlink = quebraLink(newlink);
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        } else {
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        }
                    }
                }
            }
            linkOrig = linkInit;
            link = linkOrig;
            newlink = "";
        }
        if (linkOrig.contains("?")) {
            String linkTemporary = "";
            newlink = "";
            if (linkOrig.contains("go!")) {
                linkOrig = linkOrig.replace("?go!", "?");
            }
            if (linkOrig.contains("=")) {
                for (int i = 0; i < linkOrig.length(); i++) {
                    letra = linkOrig.charAt(i);
                    if (letra == '=') {
                        for (int j = i + 1; j < linkOrig.length(); j++) {
                            newlink += linkOrig.charAt(j);
                        }
                        linkTemporary = linkTemporary.replace(".", "");
                        try {
                            linkTemporary = HexToChar(newlink);
                        } catch (Exception e) {
                            System.err.println("erro hex 1º");
                            estado = true;
                        }
                        break;
                    }
                }
                if (linkTemporary.contains("http") && !estado) {
                    newlink = "";
                    for (int i = 0; i < linkTemporary.length(); i++) {
                        letra = linkTemporary.charAt(i);
                        if (letra == 'h') {
                            for (int j = i; j < linkTemporary.length(); j++) {
                                newlink += linkTemporary.charAt(j);
                            }
                            newlink = newlink.replace("!og", "");
                            if (precisaRepassar(newlink)) {
                                newlink = quebraLink(newlink);
                                if (isValid(newlink)) {
                                    return newlink;
                                }
                            } else {
                                if (isValid(newlink)) {
                                    return newlink;
                                }
                            }
                        }
                    }
                }
                if (linkTemporary.contains("ptth") && !estado) {
                    newlink = "";
                    linkTemporary = inverteFrase(linkTemporary);
                    for (int i = 0; i < linkTemporary.length(); i++) {
                        letra = linkTemporary.charAt(i);
                        if (letra == 'h') {
                            for (int j = i; j < linkTemporary.length(); j++) {
                                newlink += linkTemporary.charAt(j);
                            }
                            newlink = newlink.replace("!og", "");
                            if (precisaRepassar(newlink)) {
                                newlink = quebraLink(newlink);
                                if (isValid(newlink)) {
                                    return newlink;
                                }
                            } else {
                                if (isValid(newlink)) {
                                    return newlink;
                                }
                            }
                        }
                    }
                }
            }
            estado = false;
            linkTemporary = "";
            for (int i = 0; i < linkOrig.length(); i++) {
                letra = linkOrig.charAt(i);
                if (letra == '?') {
                    for (int j = i + 1; j < linkOrig.length(); j++) {
                        linkTemporary += linkOrig.charAt(j);
                    }
                    linkTemporary = linkTemporary.replace(".", "");
                    try {
                        link = HexToChar(linkTemporary);
                    } catch (Exception e) {
                        System.err.println("erro hex 2º");
                        estado = true;
                    }
                    break;
                }
            }
            if (link.contains("http") && !estado) {
                newlink = "";
                for (int i = 0; i < link.length(); i++) {
                    letra = link.charAt(i);
                    if (letra == 'h') {
                        for (int j = i; j < link.length(); j++) {
                            newlink += link.charAt(j);
                        }
                        newlink = newlink.replace("!og", "");
                        if (precisaRepassar(newlink)) {
                            newlink = quebraLink(newlink);
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        } else {
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        }
                    }
                }
            }
            if (link.contains("ptth") && !estado) {
                newlink = "";
                linkTemporary = inverteFrase(link);
                for (int i = 0; i < linkTemporary.length(); i++) {
                    letra = linkTemporary.charAt(i);
                    if (letra == 'h') {
                        for (int j = i; j < linkTemporary.length(); j++) {
                            newlink += linkTemporary.charAt(j);
                        }
                        newlink = newlink.replace("!og", "");
                        if (precisaRepassar(newlink)) {
                            newlink = quebraLink(newlink);
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        } else {
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        }
                    }
                }
            }
            linkOrig = linkInit;
            link = linkOrig;
            newlink = "";
        }
        if (linkOrig.contains("?") && !linkOrig.contains("id=") && !linkOrig.contains("url=") && !linkOrig.contains("link=") && !linkOrig.contains("r=http") && !linkOrig.contains("r=ftp")) {
            for (int i = 0; i < linkOrig.length(); i++) {
                letra = linkOrig.charAt(i);
                if (letra == '?') {
                    newlink = "";
                    for (int j = i + 1; j < linkOrig.length(); j++) {
                        newlink += linkOrig.charAt(j);
                    }
                    if (newlink.contains("ptth")) {
                        newlink = inverteFrase(newlink);
                        if (precisaRepassar(newlink)) {
                            newlink = quebraLink(newlink);
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        } else {
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        }
                    } else {
                        if (precisaRepassar(newlink)) {
                            newlink = quebraLink(newlink);
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        } else {
                            if (isValid(newlink)) {
                                return newlink;
                            }
                        }
                    }
                }
            }
        }
        if ((link.contains("url=")) || (link.contains("link=")) || (link.contains("?r=http")) || (link.contains("?r=ftp"))) {
            if (!link.contains("//:ptth")) {
                for (int i = 0; i < link.length(); i++) {
                    letra = link.charAt(i);
                    if (letra == '=') {
                        for (int j = i + 1; j < link.length(); j++) {
                            letra = link.charAt(j);
                            newlink += letra;
                        }
                        break;
                    }
                }
                if (precisaRepassar(newlink)) {
                    newlink = quebraLink(newlink);
                    if (isValid(newlink)) {
                        return newlink;
                    }
                } else {
                    if (isValid(newlink)) {
                        return newlink;
                    }
                }
            }
        }
        if (linkOrig.contains("//:ptth") || linkOrig.contains("//:sptth")) {
            if (linkOrig.contains("=")) {
                for (int i = 0; i < linkOrig.length(); i++) {
                    letra = linkOrig.charAt(i);
                    if (letra == '=') {
                        for (int j = linkOrig.length() - 1; j > i; j--) {
                            letra = linkOrig.charAt(j);
                            newlink += letra;
                        }
                        break;
                    }
                }
                if (precisaRepassar(newlink)) {
                    newlink = quebraLink(newlink);
                    if (isValid(newlink)) {
                        return newlink;
                    }
                } else {
                    if (isValid(newlink)) {
                        return newlink;
                    }
                }
            }
            newlink = inverteFrase(linkOrig);
            if (precisaRepassar(newlink)) {
                newlink = quebraLink(newlink);
                if (isValid(newlink)) {
                    return newlink;
                }
            } else {
                if (isValid(newlink)) {
                    return newlink;
                }
            }
        }
        if (linkOrig.contains("?go!")) {
            linkOrig = linkOrig.replace("?go!", "?down!");
            newlink = linkOrig;
            if (precisaRepassar(newlink)) {
                newlink = quebraLink(newlink);
                if (isValid(newlink)) {
                    return newlink;
                }
            } else {
                if (isValid(newlink)) {
                    return newlink;
                }
            }
        }
        if (linkOrig.contains("down!")) {
            linkOrig = linkOrig.replace("down!", "");
            return quebraLink(linkOrig);
        }
        newlink = "";
        for (int i = linkOrig.length() - 4; i >= 0; i--) {
            letra = linkOrig.charAt(i);
            if (letra == '=') {
                for (int j = i + 1; j < linkOrig.length(); j++) {
                    newlink += linkOrig.charAt(j);
                }
                break;
            }
        }
        String ltmp = "";
        try {
            ltmp = HexToChar(newlink);
        } catch (Exception e) {
            System.err.println("erro hex 3º");
        }
        if (ltmp.contains("http://")) {
            if (precisaRepassar(ltmp)) {
                ltmp = quebraLink(ltmp);
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            } else {
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            }
        } else if (ltmp.contains("//:ptth")) {
            ltmp = inverteFrase(ltmp);
            if (precisaRepassar(ltmp)) {
                ltmp = quebraLink(ltmp);
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            } else {
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            }
        } else {
            ltmp = newlink;
        }
        ltmp = decode64(newlink);
        if (ltmp.contains("http://")) {
            if (precisaRepassar(ltmp)) {
                ltmp = quebraLink(newlink);
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            } else {
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            }
        } else if (ltmp.contains("//:ptth")) {
            ltmp = inverteFrase(ltmp);
            if (precisaRepassar(ltmp)) {
                newlink = quebraLink(newlink);
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            } else {
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            }
        } else {
            ltmp = newlink;
        }
        try {
            ltmp = decodeAscii(newlink);
        } catch (NumberFormatException e) {
            System.err.println("erro ascii");
        }
        if (ltmp.contains("http://")) {
            if (precisaRepassar(ltmp)) {
                ltmp = quebraLink(newlink);
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            } else {
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            }
        } else if (ltmp.contains("//:ptth")) {
            ltmp = inverteFrase(ltmp);
            if (precisaRepassar(ltmp)) {
                ltmp = quebraLink(ltmp);
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            } else {
                if (isValid(ltmp)) {
                    newlink = ltmp;
                    return newlink;
                }
            }
        } else {
            ltmp = null;
        }
        newlink = "";
        int cont = 0;
        letra = '\0';
        ltmp = "";
        newlink = "";
        for (int i = linkOrig.length() - 4; i >= 0; i--) {
            letra = linkOrig.charAt(i);
            if (letra == '=' || letra == '?') {
                for (int j = i + 1; j < linkOrig.length(); j++) {
                    if (linkOrig.charAt(j) == '.') {
                        break;
                    }
                    newlink += linkOrig.charAt(j);
                }
                break;
            }
        }
        ltmp = newlink;
        String tmp = "";
        String tmp2 = "";
        do {
            try {
                tmp = HexToChar(ltmp);
                tmp2 = HexToChar(inverteFrase(ltmp));
                if (!tmp.isEmpty() && tmp.length() > 5 && !tmp.contains("") && !tmp.contains("§") && !tmp.contains("�") && !tmp.contains("")) {
                    ltmp = HexToChar(ltmp);
                } else if (!inverteFrase(tmp2).isEmpty() && inverteFrase(tmp2).length() > 5 && !inverteFrase(tmp2).contains("") && !inverteFrase(tmp2).contains("§") && !inverteFrase(tmp2).contains("�")) {
                    ltmp = HexToChar(inverteFrase(ltmp));
                }
            } catch (NumberFormatException e) {
            }
            tmp = decode64(ltmp);
            tmp2 = decode64(inverteFrase(ltmp));
            if (!tmp.contains("�") && !tmp.contains("ޚ")) {
                ltmp = decode64(ltmp);
            } else if (!tmp2.contains("�") && !tmp2.contains("ޚ")) {
                ltmp = decode64(inverteFrase(ltmp));
            }
            try {
                tmp = decodeAscii(ltmp);
                tmp2 = decodeAscii(inverteFrase(ltmp));
                if (!tmp.contains("") && !tmp.contains("�") && !tmp.contains("§") && !tmp.contains("½") && !tmp.contains("*") && !tmp.contains("\"") && !tmp.contains("^")) {
                    ltmp = decodeAscii(ltmp);
                } else if (!tmp2.contains("") && !tmp2.contains("�") && !tmp2.contains("§") && !tmp2.contains("½") && !tmp2.contains("*") && !tmp2.contains("\"") && !tmp2.contains("^")) {
                    ltmp = decodeAscii(inverteFrase(ltmp));
                }
            } catch (NumberFormatException e) {
            }
            cont++;
            if (ltmp.contains("http")) {
                newlink = ltmp;
                if (precisaRepassar(newlink)) {
                    newlink = quebraLink(newlink);
                    if (isValid(newlink)) {
                        return newlink;
                    }
                } else {
                    if (isValid(newlink)) {
                        return newlink;
                    }
                }
            } else if (ltmp.contains("ptth")) {
                newlink = inverteFrase(ltmp);
                if (precisaRepassar(newlink)) {
                    newlink = quebraLink(newlink);
                    if (isValid(newlink)) {
                        return newlink;
                    }
                } else {
                    if (isValid(newlink)) {
                        return newlink;
                    }
                }
            }
        } while (!isValid(newlink) && cont <= 20);
        tmp = null;
        tmp2 = null;
        ltmp = null;
        String leitura = "";
        try {
            leitura = readHTML(linkInit);
        } catch (IOException e) {
        }
        leitura = leitura.toLowerCase();
        if (leitura.contains("trocabotao")) {
            newlink = "";
            for (int i = leitura.indexOf("trocabotao"); i < leitura.length(); i++) {
                if (Character.isDigit(leitura.charAt(i))) {
                    int tmpInt = i;
                    while (Character.isDigit(leitura.charAt(tmpInt))) {
                        newlink += leitura.charAt(tmpInt);
                        tmpInt++;
                    }
                    cod = Integer.parseInt(newlink);
                    break;
                }
            }
            if (cod != 0) {
                for (int i = 7; i < linkInit.length(); i++) {
                    letra = linkInit.charAt(i);
                    if (letra == '/') {
                        newlink = linkInit.substring(0, i + 1) + "linkdiscover.php?cod=" + cod;
                        break;
                    }
                }
                DataInputStream dat = null;
                try {
                    URL url = new URL(newlink);
                    InputStream in = url.openStream();
                    dat = new DataInputStream(new BufferedInputStream(in));
                    leitura = "";
                    int dado;
                    while ((dado = dat.read()) != -1) {
                        letra = (char) dado;
                        leitura += letra;
                    }
                    newlink = leitura.replaceAll(" ", "");
                    if (precisaRepassar(newlink)) {
                        newlink = quebraLink(newlink);
                        if (isValid(newlink)) {
                            return newlink;
                        }
                    } else {
                        if (isValid(newlink)) {
                            return newlink;
                        }
                    }
                } catch (MalformedURLException ex) {
                    System.out.println("URL mal formada.");
                } catch (IOException e) {
                } finally {
                    try {
                        if (dat != null) {
                            dat.close();
                        }
                    } catch (IOException e) {
                        System.err.println("Falha ao fechar fluxo.");
                    }
                }
            }
        }
        if (precisaRepassar(linkInit)) {
            if (linkInit.substring(8).contains("http")) {
                newlink = linkInit.substring(linkInit.indexOf("http", 8), linkInit.length());
                if (isValid(newlink)) {
                    return newlink;
                }
            }
        }
        newlink = "";
        StringBuffer strBuf = null;
        try {
            strBuf = new StringBuffer(readHTML(linkInit));
            for (String tmp3 : getLibrary()) {
                if (strBuf.toString().toLowerCase().contains(tmp3)) {
                    for (int i = strBuf.toString().indexOf(tmp3); i >= 0; i--) {
                        if (strBuf.toString().charAt(i) == '"') {
                            for (int j = i + 1; j < strBuf.length(); j++) {
                                if (strBuf.toString().charAt(j) == '"') {
                                    if (precisaRepassar(newlink)) {
                                        newlink = quebraLink(newlink);
                                        if (isValid(newlink)) {
                                            return newlink;
                                        }
                                    } else {
                                        if (isValid(newlink)) {
                                            return newlink;
                                        }
                                    }
                                } else {
                                    newlink += strBuf.toString().charAt(j);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
        }
        GUIQuebraLink.isBroken = false;
        return "Desculpe o link não pode ser quebrado.";
    }

    private static String decode64(String TextoIni) {
        String textFinal = "";
        try {
            String TextoDecript = new String(new BASE64Decoder().decodeBuffer(TextoIni));
            textFinal = TextoDecript;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textFinal;
    }

    private String inverteFrase(String frase) {
        StringBuffer newfrase = new StringBuffer(frase.length());
        for (int i = frase.length() - 1; i >= 0; i--) {
            newfrase.append(frase.charAt(i));
        }
        return newfrase.toString();
    }

    private static String HexToChar(String hex) throws NumberFormatException {
        char value[] = new char[hex.length() / 2];
        int hexLength = value.length;
        for (int i = 0; i < hexLength; i++) {
            value[i] = (char) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return new String(value);
    }

    private boolean precisaRepassar(String link) {
        if (link.contains("lockurl")) {
            return true;
        }
        if (link.length() > 8) {
            if (link.substring(8).contains("http") || link.substring(8).contains("ptth")) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private String[] getLibrary() {
        String[] library = { "megaupload.com", "fileserve", "filesonic", "hotfile.com", "uploadstation", "easy-share", "rapidshare", "mediafire", "4shared", "bitroad", "depositfiles", "filebase", "freakshare", "multiupload", "zshare", "2shared", "migre", "ul.to", "uploaded.to", "oron.com", "bitshare", "uploading", "yess.me", "megavideo", "netload", "ziddu", "filebase.to", "wupload.com", "bit.ly", "goo.gl", "up-file.com", "letitbit.net", "badongo.com", "filebase.to", "ezyfile.net", "neo-share.com", "shareflare.net", "filefactory.com", "videobb.com", "castordownloads", "lix.in", "sharingmatrix" };
        return library;
    }

    private boolean isValid(String link) {
        for (String tmp : getLibrary()) {
            if (link.toLowerCase().contains(tmp)) {
                validLink = tmp;
                return true;
            }
        }
        return false;
    }

    private String decodeAscii(String ascii) throws NumberFormatException {
        char num = 0;
        int num2 = 0;
        String recebe = "";
        String convertida = "";
        for (int j = 0; j < ascii.length(); j++) {
            num = ascii.charAt(j);
            recebe = recebe + num;
            num2 = Integer.parseInt(recebe);
            if ((num2 >= 32) && (num2 <= 319)) {
                num = (char) num2;
                convertida += num;
                num2 = 0;
                recebe = "";
            }
        }
        return convertida;
    }

    protected static String readHTML(String link) throws IOException {
        URL url = null;
        try {
            url = new URL(link);
        } catch (MalformedURLException ex) {
            System.err.println("URL mal formada!");
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuffer newData = new StringBuffer();
        String s = "";
        while (null != (s = br.readLine())) {
            newData.append(s);
        }
        br.close();
        return newData.toString();
    }

    private static String decodeCifraDeCesar(String link) {
        link = link.replaceAll("025", "a");
        link = link.replaceAll("a56", "b");
        link = link.replaceAll("bA5", "c");
        link = link.replaceAll("Bc7", "d");
        link = link.replaceAll("AdD", "e");
        link = link.replaceAll("ec3", "f");
        link = link.replaceAll("e2d", "g");
        link = link.replaceAll("a2d", "h");
        link = link.replaceAll("ge4", "i");
        link = link.replaceAll("i6A", "j");
        link = link.replaceAll("F7e", "k");
        link = link.replaceAll("kf4", "l");
        link = link.replaceAll("21F", "m");
        link = link.replaceAll("f25", "n");
        link = link.replaceAll("m91", "o");
        link = link.replaceAll("ij5", "p");
        link = link.replaceAll("j32", "q");
        link = link.replaceAll("q56", "r");
        link = link.replaceAll("f0j", "s");
        link = link.replaceAll("f0d", "t");
        link = link.replaceAll("qs0", "u");
        link = link.replaceAll("r02", "v");
        link = link.replaceAll("5fg", "w");
        link = link.replaceAll("ppN", "x");
        link = link.replaceAll("f0C", "y");
        link = link.replaceAll("56s", "z");
        link = link.replaceAll("sx0", ".");
        link = link.replaceAll("x15", "/");
        link = link.replaceAll("x5F", ":");
        link = link.replaceAll("pxw", "?");
        link = link.replaceAll("zc0", "=");
        return link;
    }
}
