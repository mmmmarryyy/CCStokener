package img_getter.img.parser;

import img_getter.Img_getterView;
import img_getter.img.handler.BaseHandler;
import img_getter.img.handler.ContentHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class ContentParser extends BaseParser {

    ContentHandler handler;

    public ContentParser(Img_getterView _view) {
        super(_view);
    }

    @Override
    public void setHandler(BaseHandler _handler) {
        handler = (ContentHandler) _handler;
        handler.reset();
    }

    @Override
    public BaseHandler getHandler() {
        return handler;
    }

    @Override
    public void parse() {
        BufferedReader br = null;
        try {
            URL url = new URL(this.getView().getUrl());
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            handler.process(br);
        } catch (MalformedURLException ex) {
            this.getView().log("不正确的URL地址！");
        } catch (IOException ioe) {
            this.getView().log("无法读取该网址！");
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    this.getView().log("无法关闭打开的URL输入流！");
                }
            }
        }
    }
}
