/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
*/

package kx;

/*
types
20+ userenums
98 table
99 dict
100 lambda
101 unary prim
102 binary prim
103 ternary(operator)
104 projection
105 composition
106 f'
107 f/
108 f\
109 f':
110 f/:
111 f\:
112 dynamic load
 */
import studio.kdb.K;
import java.util.LinkedList;
import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class c {
    DataInputStream i;
    OutputStream o;
    byte[] b, B;
    int j;
    private JFrame frame;
    int J;
    boolean a;

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }
    /*
    public void i(File f) throws IOException
    {
    FileInputStream is= new FileInputStream(f);
    i=new DataInputStream(is);
    }

    public void o(File f) throws IOException
    {
    FileOutputStream os= new FileOutputStream(f);
    o=new DataOutputStream(os);
    }
     */

    void io(Socket s) throws IOException {
        s.setTcpNoDelay(true);
        i = new DataInputStream(s.getInputStream());
        o = s.getOutputStream();
    }

    public void close() {
        try {
            // this will force k() to break out i hope
            if (i != null)
                try {
                    i.close();
                }
                catch (IOException e) {
                }
                finally {
                    i = null;
                }
            if (o != null)
                try {
                    o.close();
                }
                catch (IOException e) {
                }
                finally {
                    o = null;
                }
        }        // synchronized(this)
        finally {
            frame = null;
            closed = true;
        }
    }

    public c() {
    }

    public c(Socket s) throws IOException {
        io(s);
        i.read(b = new byte[99]);
        o.write(b,0,1);
    }

    public c(ServerSocket s) throws IOException {
        this(s.accept());
    }

    public static class K4AccessException extends Exception {
        K4AccessException(String s) {
            super(s);
        }
    }
    java.util.List responses = java.util.Collections.synchronizedList(new LinkedList());
    boolean closed = true;

    public boolean isClosed() {
        return closed;
    }

    public K.KBase getResponse() throws Throwable {
        Object o;

        synchronized (responses) {
            if (responses.size() == 0)
                try {
                    responses.wait();
                }
                catch (InterruptedException e) {
                }

            o = responses.remove(0);
        }

        if (o instanceof Throwable)
            throw (Throwable) o;

        return (K.KBase) o;
    }

    private void startReader() {
        Runnable runner = new Runnable() {
            public void run() {
                while (!closed) {
                    Object o = null;

                    try {
                        o = k();
                    }
                    catch (K4Exception e) {
                        o = e;
                    }
                    catch (Throwable t) {
                        o = t;
                        close();
                    }

                    synchronized (responses) {
                        responses.add(o);
                        responses.notify();
                    }
                }
            }
        };

        Thread t = new Thread(runner);
        t.start();
    }

    public void reconnect() throws IOException,K4Exception {
        io(new Socket(host,port));

        java.io.ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.io.DataOutputStream dos = new DataOutputStream(baos);
        dos.write(up.getBytes());
        dos.writeByte(0);
        dos.flush();
        byte[] b = baos.toByteArray();
        o.write(baos.toByteArray());

        if (1 != i.read(new byte[1],0,1))
            throw new K4Exception("Authentication failed");
        closed = false;
        startReader();
    }
    private String host;
    private int port;
    private String up;

    public c(String h,int p,String u) {
        host = h;
        port = p;
        up = u;
    }

    boolean rb() {
        return 1 == b[j++];
    }

    short rh() {
        int x = b[j++], y = b[j++];
        return (short) (a ? x & 0xff | y << 8 : x << 8 | y & 0xff);
    }

    int ri() {
        int x = rh(), y = rh();
        return a ? x & 0xffff | y << 16 : x << 16 | y & 0xffff;
    }

    long rj() {
        int x = ri(), y = ri();
        return a ? x & 0xffffffffL | (long) y << 32 : (long) x << 32 | y & 0xffffffffL;
    }

    float re() {
        return Float.intBitsToFloat(ri());
    }

    double rf() {
        return Double.longBitsToDouble(rj());
    }

    char rc() {
        return (char) (b[j++] & 0xff);
    }

    K.KSymbol rs() {
        int k = j;
        for (;b[k] != 0;)
            ++k;
        char[] s = new char[k - j];
        for (int i = 0;j < k;)
            s[i++] = (char) (0xFF & b[j++]);
        ++j;
        return new K.KSymbol(new String(s));
    }

    K.UnaryPrimitive rup() {
        return new K.UnaryPrimitive(b[j++]);
    }

    K.BinaryPrimitive rbp() {
        return new K.BinaryPrimitive(b[j++]);
    }

    K.TernaryOperator rternary() {
        return new K.TernaryOperator(b[j++]);
    }

    K.Function rfn() {
        K.KSymbol s = rs();
        return new K.Function((K.KCharacterVector) r());
    }

    K.Feach rfeach() {
        return new K.Feach(r());
    }

    K.Fover rfover() {
        return new K.Fover(r());
    }

    K.Fscan rfscan() {
        return new K.Fscan(r());
    }

    K.FComposition rcomposition() {
        int n = ri();
        Object[] objs = new Object[n];
        for (int i = 0;i < n;i++)
            objs[i] = r();

        return new K.FComposition(objs);
    }

    K.FPrior rfPrior() {
        return new K.FPrior(r());
    }

    K.FEachRight rfEachRight() {
        return new K.FEachRight(r());
    }

    K.FEachLeft rfEachLeft() {
        return new K.FEachLeft(r());
    }

    K.Projection rp() {
        int n = ri();
        K.KList list = new K.KList(n);
        K.KBase[] array = (K.KBase[]) list.getArray();
        for (int i = 0;i < n;i++)
            array[i] = r();

        return new K.Projection(list);
    }

    K.Minute ru() {
        return new K.Minute(ri());
    }

    K.Month rm() {
        return new K.Month(ri());
    }

    K.Second rv() {
        return new K.Second(ri());
    }

    K.KTime rt() {
        return new K.KTime(ri());
    }

    K.KDate rd() {
        return new K.KDate(ri());
    }

    K.KTimestamp rz() {
        return new K.KTimestamp(rf());
    }

    K.KBase r() {
        int i = 0, n, t = b[j++];
        if (t < 0)
            switch (t) {
                case -1:
                    return new K.KBoolean(rb());
                case -4:
                    return new K.KByte(b[j++]);
                case -5:
                    return new K.KShort(rh());
                case -6:
                    return new K.KInteger(ri());
                case -7:
                    return new K.KLong(rj());
                case -8:
                    return new K.KFloat(re());
                case -9:
                    return new K.KDouble(rf());
                case -10:
                    return new K.KCharacter(rc());
                case -11:
                    return rs();
                case -13:
                    return rm();
                case -14:
                    return rd();
                case -15:
                    return rz();
                case -17:
                    return ru();
                case -18:
                    return rv();
                case -19:
                    return rt();
            }

        if (t == 100)
            return rfn(); // fn - lambda
        if (t == 101)
            return rup();  // unary primitive
        if (t == 102)
            return rbp();  // binary primitive
        if (t == 103)
            return rternary();
        if (t == 104)
            return rp(); // fn projection
        if (t == 105)
            return rcomposition();

        if (t == 106)
            return rfeach(); // f'
        if (t == 107)
            return rfover(); // f/
        if (t == 108)
            return rfscan(); //f\
        if (t == 109)
            return rfPrior(); // f':
        if (t == 110)
            return rfEachRight(); // f/:
        if (t == 111)
            return rfEachLeft(); // f\:
        if (t == 112) {
            // dynamic load
            j++;
            return null;
        }
        if (t > 99) {
            j++;
            return null;
        }
        if (t == 99)
            return new K.Dict(r(),r());
        byte attr = b[j++];
        if (t == 98)
            return new K.Flip((K.Dict) r());
        n = ri();
        switch (t) {
            case 0: {
                K.KList L = new K.KList(n);
                L.setAttr(attr);
                K.KBase[] array = (K.KBase[]) L.getArray();
                for (;i < n;i++)
                    array[i] = r();
                return L;
            }
            case 1: {
                K.KBooleanVector B = new K.KBooleanVector(n);
                B.setAttr(attr);
                boolean[] array = (boolean[]) B.getArray();
                for (;i < n;i++)
                    array[i] = rb();
                return B;
            }
            case 4: {
                K.KByteVector G = new K.KByteVector(n);
                G.setAttr(attr);
                byte[] array = (byte[]) G.getArray();
                for (;i < n;i++)
                    array[i] = b[j++];
                return G;
            }
            case 5: {
                K.KShortVector H = new K.KShortVector(n);
                H.setAttr(attr);
                short[] array = (short[]) H.getArray();
                for (;i < n;i++)
                    array[i] = rh();
                return H;
            }
            case 6: {
                K.KIntVector I = new K.KIntVector(n);
                I.setAttr(attr);
                int[] array = (int[]) I.getArray();
                for (;i < n;i++)
                    array[i] = ri();
                return I;
            }
            case 7: {
                K.KLongVector J = new K.KLongVector(n);
                J.setAttr(attr);
                long[] array = (long[]) J.getArray();
                for (;i < n;i++)
                    array[i] = rj();
                return J;
            }
            case 8: {
                K.KFloatVector E = new K.KFloatVector(n);
                E.setAttr(attr);
                float[] array = (float[]) E.getArray();
                for (;i < n;i++)
                    array[i] = re();
                return E;
            }
            case 9: {
                K.KDoubleVector F = new K.KDoubleVector(n);
                F.setAttr(attr);
                double[] array = (double[]) F.getArray();
                for (;i < n;i++)
                    array[i] = rf();
                return F;
            }
            case 10: {
                K.KCharacterVector C = new K.KCharacterVector(n);
                C.setAttr(attr);
                char[] array = (char[]) C.getArray();
                for (;i < n;i++)
                    array[i] = rc();
                return C;
            }
            case 11: {
                K.KSymbolVector S = new K.KSymbolVector(n);
                S.setAttr(attr);
                String[] array = (String[]) S.getArray();
                for (;i < n;i++)
                    array[i] = rs().s;
                return S;
            }
            case 13: {
                K.KMonthVector M = new K.KMonthVector(n);
                M.setAttr(attr);
                int[] array = (int[]) M.getArray();
                for (;i < n;i++)
                    array[i] = ri();
                return M;
            }
            case 14: {
                K.KDateVector D = new K.KDateVector(n);
                D.setAttr(attr);
                int[] array = (int[]) D.getArray();
                for (;i < n;i++)
                    array[i] = ri();
                return D;
            }
            case 17: {
                K.KMinuteVector U = new K.KMinuteVector(n);
                U.setAttr(attr);
                int[] array = (int[]) U.getArray();
                for (;i < n;i++)
                    array[i] = ri();
                return U;
            }
            case 15: {
                K.KTimestampVector Z = new K.KTimestampVector(n);
                Z.setAttr(attr);
                double[] array = (double[]) Z.getArray();
                for (;i < n;i++)
                    array[i] = rf();
                return Z;
            }
            case 18: {
                K.KSecondVector V = new K.KSecondVector(n);
                V.setAttr(attr);
                int[] array = (int[]) V.getArray();
                for (;i < n;i++)
                    array[i] = ri();
                return V;
            }
            case 19: {
                K.KTimeVector T = new K.KTimeVector(n);
                T.setAttr(attr);
                int[] array = (int[]) T.getArray();
                for (;i < n;i++)
                    array[i] = ri();
                return T;
            }
        }
        return null;
    }

    void w(int i,K.KBase x) throws IOException {
        java.io.ByteArrayOutputStream baosBody = new ByteArrayOutputStream();
        java.io.DataOutputStream dosBody = new DataOutputStream(baosBody);
        x.serialise(dosBody);

        java.io.ByteArrayOutputStream baosHeader = new ByteArrayOutputStream();
        java.io.DataOutputStream dosHeader = new DataOutputStream(baosHeader);
        dosHeader.writeByte(0);
        dosHeader.writeByte(i);
        dosHeader.writeByte(0);
        dosHeader.writeByte(0);
        int msgSize = 8 + dosBody.size();
        K.write(dosHeader,msgSize);
        byte[] b = baosHeader.toByteArray();
        o.write(b);
        b = baosBody.toByteArray();
        o.write(b);
    }

    public static class K4Exception extends Exception {
        K4Exception(String s) {
            super(s);
        }
    }


    /*
    public synchronized Object k() throws K4Exception, IOException
    {
    i.readFully(b = new byte[8]);
    a = b[0] == 1;
    j = 4;
    i.readFully(b = new byte[ri() - 8]);
    if (b[0] == -128)
    {
    j = 1;
    throw new K4Exception(rs());
    }
    j = 0;
    return r();
    }
     */
    public synchronized Object k() throws K4Exception,IOException {
        i.readFully(b = new byte[8]);
        int msgType = b[1];

        a = b[0] == 1;
        j = 4;

        final int msgLength = ri() - 8;

        final String message = "Receiving data ...";
        final String note = "0 of " + (msgLength / 1024) + " kB";
        String title = "Studio for kdb+";
        UIManager.put("ProgressMonitor.progressText",title);

        final int min = 0;
        final int max = msgLength;
        final ProgressMonitor pm = new ProgressMonitor(frame,message,note,min,max);

        try {
            pm.setMillisToDecideToPopup(300);
            pm.setMillisToPopup(100);
            pm.setProgress(0);

            b = new byte[msgLength];
            int total = 0;
            int packetSize = 1 + msgLength / 100;
            if (packetSize < 8192)
                packetSize = 8192;

            while (total < msgLength) {
                if (pm.isCanceled())
                    throw new IOException("Cancelled by user");

                int remainder = msgLength - total;
                if (remainder < packetSize)
                    packetSize = remainder;

                total += i.read(b,total,packetSize);
                final int _total = total;
                final String _note = (total / 1024) + " of " + (msgLength / 1024) + " kB";
                SwingUtilities.invokeLater(new Runnable() {
                                           public void run() {
                                               pm.setProgress(_total);
                                               pm.setNote(_note);
                                           }
                                       });
            }
        }
        finally {
            pm.close();
        }

        if (b[0] == -128) {
            j = 1;
            throw new K4Exception(rs().toString(true));
        }
        j = 0;
        return r();
    }

    /*
    public synchronized K.KBase k() throws K4Exception, IOException
    { 
    while( true)
    {
    i.readFully(b = new byte[8]);
    int msgType=b[1];

    a= b[0] == 1;
    j= 4;
    int msgSize=ri() - 8;

    if(statusObserver!=null)
    statusObserver.setMsgSize(msgType,msgSize);
    b= new byte[msgSize];
    int total=0;
    while(total<msgSize)
    {
    // check whether -1 can be returned
    total+=i.read(b,total,msgSize-total);
    if(statusObserver!=null)
    statusObserver.setProgress(msgType,total);
    }

    if (b[0] == -128)
    {
    j = 1;
    throw new K4Exception(rs().s);
    }
    j = 0;

    if( msgType == 2)
    {
    K.KBase r=r();
    if(msgHandler != null)
    {
    switch(msgType)
    {
    case(0):{msgHandler.asyncMessage(r,msgSize);}break;
    case(1):{msgHandler.syncMessage(r,msgSize);}break;
    case(2):{msgHandler.responseMessage(r,msgSize);}break;
    };
    }

    if(statusObserver!=null)
    statusObserver.completed(msgType);

    return r;
    }
    }
    }
     */
    public void k(K.KBase x) throws K4Exception,IOException {
        w(1,x);
    }
}