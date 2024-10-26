package com.mainatom.utils;

import org.xml.sax.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;

/**
 * Загрузчик из Reader. Реализует набор сервисных методов для загрузки из различных источников,
 * превращает источники в Reader и вызывает loadReaderHandler для загрузки.
 * После загрузки любой источник закрывается.
 */
public class LoadReader {

    private LoadReaderHandler _handler;

    /**
     * Откуда грузить данные потомкам
     */
    private Reader _reader;

    /**
     * Если чтение идет из файла, имя этого файла
     */
    private String _filename;

    /**
     * Кодировка загружаемого потока
     */
    private String _charset = Charset.defaultCharset().toString();

    /**
     * Если загрузка идет через stream, то тут этот stream
     */
    private InputStream _stream;

    private InputSource _inputSource;

    public LoadReader(LoadReaderHandler handler) {
        _handler = handler;
    }

    /**
     * Через какой объект производить чтение
     */
    public LoadReaderHandler getHandler() {
        return _handler;
    }

    /**
     * Имя загружаемого файла (может отсутствовать)
     */
    public String getFilename() {
        return _filename == null ? "" : _filename;
    }

    /**
     * Имя загружаемого файла. В случае, когда загрузка идет не из реального файла,
     * этим методом можно установить виртуальное имя файла для идентификации при ошибках.
     *
     * @param filename имя файла
     */
    public void setFilename(String filename) {
        _filename = filename;
    }

    /**
     * Кодировка загружаемого файла (по умолчанию - системная)
     */
    public String getCharset() {
        return _charset;
    }

    /**
     * Через какой Reader читать. Используется в обработчике.
     */
    public Reader getReader() {
        return _reader;
    }

    /**
     * Если загрузка идет через InputStream (из файла, из потока, из ресурса ...), то
     * этот метод возвращает оригинальный поток. Если нет (из строки, из Reader) -
     * то возвращает null.
     */
    public InputStream getStream() {
        return _stream;
    }

    /**
     * Возвращает InputSource для объекта.
     * Если определен getStream(), то для него, иначе для getReader().
     */
    public InputSource getInputSource() {
        if (_inputSource != null) {
            return _inputSource;
        }
        if (getStream() == null) {
            _inputSource = new InputSource(getReader());
        } else if (getReader() != null) {
            _inputSource = new InputSource(getStream());
        }
        return _inputSource;
    }

    /**
     * Вызов обработчика загрузки
     */
    protected void onLoad() throws Exception {
        getHandler().onLoad(this);
    }

    /**
     * Загрузка из reader. Все вызовы методом так или иниче приводят сюда
     *
     * @param reader откуда грузить
     * @throws Exception
     */
    protected void loadReader(Reader reader) throws Exception {
        if (!(reader instanceof BufferedReader)) {
            this._reader = new BufferedReader(reader);
        } else {
            this._reader = reader;
        }
        try {
            try {
                onLoad();
            } finally {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            if (getFilename().length() > 0) {
                throw new Exception(String.format("Error load file [%s]", getFilename()), e);
            } else {
                throw e;
            }
        }
    }

    /**
     * Загрузка потока. Все вызовы, которые приводят к загрузке потока приводят сюда.
     * Отсюда формируется reader и вызывается loadReader
     *
     * @param strm поток
     * @throws Exception
     */
    protected void loadStream(InputStream strm) throws Exception {
        _stream = strm;
        loadReader(new InputStreamReader(strm, getCharset()));
    }

    public void fromBytes(byte[] bytes) throws Exception {
        fromBytes(bytes, Charset.defaultCharset().toString());
    }

    public void fromBytes(byte[] bytes, String charset) throws Exception {
        _charset = charset;
        loadStream(new ByteArrayInputStream(bytes));
    }

    public void fromFile(String filename) throws Exception {
        fromFile(new File(filename), Charset.defaultCharset().toString());
    }

    public void fromFile(String filename, String charset) throws Exception {
        fromFile(new File(filename), charset);
    }

    public void fromFile(File file) throws Exception {
        fromFile(file, Charset.defaultCharset().toString());
    }

    public void fromFile(File file, String charset) throws Exception {
        _filename = file.getAbsolutePath();
        _charset = charset;
        loadStream(new FileInputStream(file));
    }

    public void fromReader(Reader reader) throws Exception {
        loadReader(reader);
    }

    public void fromStream(InputStream stream) throws Exception {
        fromStream(stream, Charset.defaultCharset().toString());
    }

    public void fromStream(InputStream stream, String charset) throws Exception {
        _charset = charset;
        loadStream(stream);
    }

    public void fromString(String data) throws Exception {
        loadReader(new StringReader(data));
    }

    public void fromURL(URL url) throws Exception {
        fromURL(url, Charset.defaultCharset().toString());
    }

    public void fromURL(URL url, String charset) throws Exception {
        _charset = charset;
        loadStream(url.openStream());
    }

    public void fromResource(String name) throws Exception {
        URL url = getClass().getResource(name);
        if (url == null) {
            throw new Exception("Resource [" + name + "] not found");
        }
        fromURL(url);
    }

    public void fromResource(String name, String charset) throws Exception {
        URL url = getClass().getResource(name);
        if (url == null) {
            throw new Exception("Resource [" + name + "] not found");
        }
        fromURL(url, charset);
    }
}
