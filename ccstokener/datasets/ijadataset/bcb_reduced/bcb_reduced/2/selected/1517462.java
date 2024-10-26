package jp.co.lattice.vProcessor.base;

import java.io.*;
import java.util.zip.*;
import java.net.URL;
import java.util.Hashtable;
import jp.co.lattice.vProcessor.node.*;
import jp.co.lattice.vProcessor.com.*;
import jp.co.lattice.vKernel.core.b0.*;
import jp.co.lattice.vKernel.core.c0.*;

/**
 * ��ʊK�w���� Java XVL3 Processor �Ɉ�n���f�[�^�̃N���X
 * @author	  created by Eishin Matsui (00/03/11-)
 */
public class x3pToProcessor extends x3pRoot {

    /** XVL3�t�@�C��URL --- ����l null				*/
    public URL xvl3FileUrl = null;

    /** �Ő���i 2�̔{�� �j --- ����l�F4		*/
    public int numDivEdgeOnGreg = 4;

    /**
	 * static�ϐ���p�̂��߂̓����N���X
	 */
    public static class Global {

        public x3pElement[] topElms = null;

        public Hashtable xmlIdTable = null;

        public int numDivEdgeOnGreg = 4;

        /**
		 * �R���X�g���N�^
		 * @param  dt		(( I )) �O���[�o���f�[�^
		 */
        public Global(x3pGlobal dt) {
        }
    }

    /** ���N���X�p�̃O���[�o���f�[�^		*/
    private final Global Gbl() {
        return ((x3pBaseGblElm) global.GBaseX3p()).gToProcessor;
    }

    /**
	 * �R���X�g���N�^
	 * @param  dt		(( I )) �O���[�o���f�[�^
	 */
    public x3pToProcessor(x3pGlobal dt) {
        super(dt);
    }

    /** lvError�p�̃O���[�o���f�[�^				*/
    private final lvError.Global ErrProc() {
        return ((lvComGblElm) global.GCom()).gError;
    }

    /**
	 * Java XVL3 Processor �Ɉ�n���f�[�^���Z�b�g����					<br>
	 * ���̊֐����s��A�I�u�W�F�N�g��ێ���������K�v�͂Ȃ�
	 * @return				lvConst.LV_SUCCESS �܂��� lvConst.LV_FAILURE
	 */
    public final boolean SetData() {
        try {
            ErrProc().BeginThrowMode();
            SetDataMain();
            ErrProc().EndThrowMode();
            return lvConst.LV_SUCCESS;
        } catch (lvThrowable exception) {
            ErrProc().EndThrowMode();
            return lvConst.LV_FAILURE;
        }
    }

    private final void SetDataMain() throws lvThrowable {
        Gbl().numDivEdgeOnGreg = numDivEdgeOnGreg;
        ExecParse();
        ExecTraverse();
    }

    private final void ExecParse() throws lvThrowable {
        x3pParseEx ps = null;
        try {
            BufferedInputStream bufInStream = new BufferedInputStream(xvl3FileUrl.openStream());
            if (isGZipped(xvl3FileUrl) == true) {
                InputStream in = new GZIPInputStream(bufInStream);
                ps = new x3pParseEx(global, in);
            } else ps = new x3pParseEx(global, bufInStream);
            ps.Parse();
            Gbl().topElms = ps.GetTopElms();
            Gbl().xmlIdTable = ps.GetXmlIdTable();
        } catch (IOException e) {
            Err().Assert(false, e.getMessage());
        }
    }

    /**
	 *	GZip���`�F�b�N
	 */
    private final boolean isGZipped(URL url) throws lvThrowable {
        boolean isZipped = false;
        try {
            InputStream in = url.openStream();
            byte header[] = new byte[2];
            in.read(header);
            if (header[0] == 31 && header[1] == -117) isZipped = true;
            in.close();
        } catch (IOException e) {
            Err().Assert(false, e.getMessage());
        }
        return isZipped;
    }

    private final void ExecTraverse() throws lvThrowable {
        x3pTrvNumNewVtxNo xvl0 = new x3pTrvNumNewVtxNo(global);
        xvl0.NumNewVtxNo(Gbl().topElms);
        x3pTrvNewVtxNo xvl1 = new x3pTrvNewVtxNo(global);
        xvl1.NewVtxNo(Gbl().topElms);
        x3pTrvAddElm0 xvl2 = new x3pTrvAddElm0(global);
        xvl2.AddElement(Gbl().topElms);
        x3pTrvAddElm1 xvl3 = new x3pTrvAddElm1(global);
        xvl3.AddElement(Gbl().topElms);
        x3pTrvToKernel xvl4 = new x3pTrvToKernel(global);
        xvl4.ToKernel(Gbl().topElms, Gbl().numDivEdgeOnGreg);
    }
}
