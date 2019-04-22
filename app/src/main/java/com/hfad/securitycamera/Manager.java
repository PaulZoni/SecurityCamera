package com.hfad.securitycamera;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;


public class Manager {

    private static Manager network;
    private Socket socket = null;
    private JSONObject obj;
    private final String BINARY = "binary";

    private Manager() {}

    public void construct() {
        try {
            socket = IO.socket("http://192.168.43.122:8000/send");
            socket.connect();
            obj = new JSONObject();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static Manager create() {
        if (network == null) {
            network = new Manager();
        }
        return network;
    }

    public void send(byte[] array) {
        try {
            obj.put(BINARY, array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("mes", obj);
    }
}
