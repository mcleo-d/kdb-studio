package studio.kdb;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import kx.c.K4Exception;
import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.core.IAuthenticationMechanism;

public class ConnectionPool {
    private static final ConnectionPool instance = new ConnectionPool();
    private final Map<Server, List<kx.c>> freeMap = new HashMap<>();
    private final Map<Server, List<kx.c>> busyMap = new HashMap<>();

    public static ConnectionPool getInstance() {
        return instance;
    }

    private ConnectionPool() {
    }

    public synchronized void purge(Server s) {
        List<kx.c> list = freeMap.computeIfAbsent(s, k -> new LinkedList<>());
        for (kx.c c : list) {
            c.close();
        }
        list.clear();
        busyMap.put(s, new LinkedList<>());
    }

    public synchronized kx.c leaseConnection(Server s) throws IOException, K4Exception {
        List<kx.c> list = freeMap.computeIfAbsent(s, k -> new LinkedList<>());
        List<kx.c> dead = new LinkedList<>();

        kx.c c = null;
        for (kx.c value : list) {
            if (!value.isClosed()) {
                c = value;
                break;
            }
            dead.add(c);
        }

        list.removeAll(dead);

        if (c == null) {
            try {
                String mech = s.getAuthenticationMechanism();
                Class<?> clazz = AuthenticationManager.getInstance().lookup(mech);
                if (clazz == null) {
                    throw new RuntimeException("Can't find authentication mechanism: " + mech);
                }
                IAuthenticationMechanism authenticationMechanism =
                    (IAuthenticationMechanism) clazz.newInstance();

                authenticationMechanism.setProperties(s.getAsProperties());
                Credentials credentials = authenticationMechanism.getCredentials();
                if (credentials.getUsername().length() > 0) {
                    String p = credentials.getPassword();

                    c = new kx.c(s.getHost(), s.getPort(),
                        credentials.getUsername() + ((p.length() == 0) ? "" : ":" + p),
                        s.getUseTLS());
                } else {
                    c = new kx.c(s.getHost(), s.getPort(), "", s.getUseTLS());
                }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException ex) {
                System.err.println("Failed to initialize connection: " + ex);
                ex.printStackTrace(System.err);
                return null;
            }
        } else {
            list.remove(c);
        }

        list = busyMap.computeIfAbsent(s, k -> new LinkedList<>());
        list.add(c);

        return c;
    }

    public synchronized void freeConnection(Server s, kx.c c) {
        if (c == null) {
            return;
        }

        List<kx.c> list = busyMap.computeIfAbsent(s, k -> new LinkedList<>());

        // If c not in our busy list it has been purged, so close it
        if (!list.remove(c)) {
            c.close();
        }

        if (!c.isClosed()) {
            list = freeMap.get(s);
            if (list == null) {
                c.close();
            } else {
                list.add(c);
            }
        }
    }

    public void checkConnected(kx.c c) throws IOException, K4Exception {
        if (c == null) {
            return;
        }
        if (c.isClosed()) {
            c.reconnect(true);
        }
    }
}
