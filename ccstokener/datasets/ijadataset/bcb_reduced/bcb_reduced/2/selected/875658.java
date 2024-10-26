package slide.show;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import cartago.GUARD;
import cartago.IBlockingCmd;
import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import jaca.android.dev.ActivityArtifact;
import jaca.android.dev.JaCaActivity;

/**
 * 
 * @author mguidi
 *
 */
public class SlideShowActivityArtifact extends ActivityArtifact {

    private SlideShowActivity mActivity;

    private String mUrl;

    private int mIndex;

    private byte[][] mData;

    private int mN;

    private String mPrefix;

    private String mPostfix;

    protected void init(JaCaActivity activity, Bundle arg1) {
        super.init(activity, arg1);
        mActivity = (SlideShowActivity) activity;
        mUrl = "";
        mIndex = 0;
        try {
            InputStream is = new URL("http://dl.dropbox.com/u/11954389/index").openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            mN = Integer.parseInt(br.readLine());
            mPrefix = br.readLine();
            mPostfix = br.readLine();
            br.close();
            is.close();
            mData = new byte[mN][];
            execInternalOp("downloadImage", mIndex);
            linkOnOptionsItemSelectedToOp("onOptionsItemSelected");
            linkOnActivityResultEventToOp("onActivityResult");
            linkOnFlingToOp("onFling");
        } catch (MalformedURLException e) {
            failed(e.getLocalizedMessage());
        } catch (IOException e) {
            failed(e.getLocalizedMessage());
        }
    }

    @GUARD
    boolean isDownloaded(int i) {
        return mData[i] != null;
    }

    @INTERNAL_OPERATION
    void downloadImage(int index) {
        await(new ImageDownloader(index));
        mUrl = mPrefix + mIndex + mPostfix;
        Bitmap bmp = BitmapFactory.decodeByteArray(mData[mIndex], 0, mData[mIndex].length);
        mActivity.setImage(bmp);
        await(new ImageDownloader((index + 1) % mN));
        await(new ImageDownloader((index - 1) >= 0 ? (index - 1) % mN : mN - 1));
    }

    class ImageDownloader implements IBlockingCmd {

        private int mI;

        public ImageDownloader(int i) {
            mI = i;
        }

        @Override
        public void exec() {
            try {
                if (mData[mI] == null) {
                    String url = mPrefix + mI + mPostfix;
                    InputStream is = new URL(url).openStream();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[16384];
                    int count;
                    while ((count = is.read(buffer)) > 0) out.write(buffer, 0, count);
                    out.flush();
                    is.close();
                    mData[mI] = out.toByteArray();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @INTERNAL_OPERATION
    void onFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
        boolean slide = false;
        final float absX = Math.abs(velocityX);
        final float absY = Math.abs(velocityY);
        if (velocityX > 500 && absY < absX) {
            mIndex = (++mIndex) % mN;
            slide = true;
        } else if (velocityX < -500 && absY < absX) {
            mIndex = (mIndex - 1) >= 0 ? (--mIndex) % mN : mN - 1;
            slide = true;
        } else if (velocityY < -500 && absX < absY) {
        } else if (velocityY > 500 && absX < 200) {
        } else if (absX > 800 || absY > 800) {
        }
        if (slide) {
            execInternalOp("downloadImage", mIndex);
            await("isDownloaded", mIndex);
            mUrl = mPrefix + mIndex + mPostfix;
            Bitmap bmp = BitmapFactory.decodeByteArray(mData[mIndex], 0, mData[mIndex].length);
            mActivity.setImage(bmp);
            signal("next_slide", mData[mIndex], mUrl);
        }
    }

    @INTERNAL_OPERATION
    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                String[] deviceInfo = contents.split(";");
                signal("device_fouded", deviceInfo[0], deviceInfo[1]);
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
        }
    }

    @INTERNAL_OPERATION
    void onOptionsItemSelected(MenuItem item) {
        signal("search_device");
    }

    @OPERATION
    void showMessage(String msg) {
        mActivity.showMessage(msg);
    }

    @OPERATION
    void getCurrentSlideData(OpFeedbackParam<byte[]> data) {
        data.set(mData[mIndex]);
    }

    @OPERATION
    void getCurrentSlideUrl(OpFeedbackParam<String> url) {
        url.set(mUrl);
    }
}
