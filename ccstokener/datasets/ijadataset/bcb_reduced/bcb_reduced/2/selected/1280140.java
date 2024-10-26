package kotan.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import kotan.AppEngineEnv;
import kotan.AuthInfo;
import kotan.Kotan;
import kotan.datastore.api.Entity;
import kotan.datastore.api.Key;
import kotan.model.EntityModel;
import kotan.model.KindModel;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;

public abstract class EntityPersistentManager {

    protected final HttpClientManager clientManager;

    public EntityPersistentManager(HttpClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public abstract void authorize(AuthInfo authInfo) throws AuthoricationRequiredException;

    protected HttpHost getHttpHost() {
        return clientManager.getHttpHost();
    }

    protected HttpHost getHttpsHost() {
        return clientManager.getHttpsHost();
    }

    public KindModel load(String kind) throws AuthoricationRequiredException {
        try {
            return new KindModel(kind, dump(kind));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void upload(EntityModel model) {
        try {
            AppEngineEnv env = Kotan.get().getEnv();
            HttpHost target = new HttpHost(env.getHost(), env.getPort(), "http");
            HttpPost httpPost = new HttpPost("/_kotan/upload");
            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("entity", new InputStreamBody(new ByteArrayInputStream(toBinary(model.getEntity())), "entity"));
            httpPost.setEntity(reqEntity);
            HttpResponse response = clientManager.httpClient.execute(target, httpPost);
            System.out.println(response.getStatusLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(Key key) {
        try {
            AppEngineEnv env = Kotan.get().getEnv();
            HttpHost target = new HttpHost(env.getHost(), env.getPort(), "http");
            HttpPost httpPost = new HttpPost("/_kotan/delete");
            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("key", new InputStreamBody(new ByteArrayInputStream(toBinary(key)), "key"));
            httpPost.setEntity(reqEntity);
            HttpResponse response = clientManager.httpClient.execute(target, httpPost);
            System.out.println(response.getStatusLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<Entity> dump(String kind) throws IOException, AuthoricationRequiredException {
        AppEngineEnv env = Kotan.get().getEnv();
        HttpHost target = new HttpHost(env.getHost(), env.getPort(), "http");
        HttpGet httpGet = new HttpGet("/_kotan/load?kind=" + kind);
        HttpResponse response = clientManager.httpClient.execute(target, httpGet);
        HttpEntity entity = response.getEntity();
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            throw new AuthoricationRequiredException();
        }
        List<Entity> list = new ArrayList<Entity>();
        if (entity != null) {
            ObjectInputStream input = null;
            try {
                input = new ObjectInputStream(entity.getContent());
                while (true) {
                    Entity e = (Entity) input.readObject();
                    list.add(e);
                }
            } catch (java.io.EOFException e) {
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (input != null) input.close();
            }
        }
        return list;
    }

    private byte[] toBinary(Serializable obj) throws IOException {
        ObjectOutputStream output = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            output = new ObjectOutputStream(out);
            output.writeObject(obj);
            output.flush();
            return out.toByteArray();
        } finally {
            if (output != null) output.close();
        }
    }
}
