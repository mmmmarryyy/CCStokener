package hoge.photostore.get;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class GetActivity extends ListActivity {

    private static final String URL_IMAGEINFOLIST = "http://photo-storage.appspot.com/list";

    private List<ImageInfo> mImageInfoList = null;

    static ArrayList<String> mFileNames = new ArrayList<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mFileNames));
        getListView().setTextFilterEnabled(true);
    }

    @Override
    protected void onResume() {
        if (mImageInfoList == null) {
            mImageInfoList = getImageInfoList(URL_IMAGEINFOLIST);
            int size = mImageInfoList.size();
            for (int i = 0; i < size; i++) {
                ImageInfo imageInfo = mImageInfoList.get(i);
                mFileNames.add(imageInfo.filename);
            }
        }
        super.onResume();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        startImageView1(position);
    }

    private void startImageView1(int i) {
        Intent intent = new Intent(this, ImageActivity.class);
        Bundle extras = new Bundle();
        ImageInfo imageInfo = mImageInfoList.get(i);
        extras.putString("key", imageInfo.key);
        extras.putString("filename", imageInfo.filename);
        intent.putExtras(extras);
        startActivity(intent);
    }

    public static final List<ImageInfo> getImageInfoList(final String urlStr) {
        ArrayList<ImageInfo> list = new ArrayList<ImageInfo>();
        byte[] line = new byte[1024];
        int byteSize = 0;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.connect();
            InputStream is = con.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((byteSize = is.read(line)) > 0) {
                out.write(line, 0, byteSize);
            }
            String[] outList = out.toString().split("\n", 0);
            int i = 0;
            while (i < outList.length) {
                String[] outList2 = outList[i].toString().split("\t", 0);
                ImageInfo imageInfo = new ImageInfo();
                System.out.println(i + "番目の要素 = :" + outList2[0]);
                imageInfo.key = outList2[0];
                System.out.println(i + "番目の要素 = :" + outList2[1]);
                imageInfo.filename = outList2[1];
                i++;
                list.add(imageInfo);
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
