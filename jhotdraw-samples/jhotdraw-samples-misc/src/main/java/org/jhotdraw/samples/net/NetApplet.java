/*
 * @(#)NetApplet.java
 *
 * Copyright (c) 1996-2010 The authors and contributors of JHotDraw.
 * You may not use, copy or modify this file, except in compliance with the
 * accompanying license terms.
 */
package org.jhotdraw.samples.net;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import org.jhotdraw.draw.Drawing;
import org.jhotdraw.draw.figure.ImageFigure;
import org.jhotdraw.draw.figure.TextFigure;
import org.jhotdraw.draw.io.DOMStorableInputOutputFormat;
import org.jhotdraw.draw.io.ImageInputFormat;
import org.jhotdraw.draw.io.ImageOutputFormat;
import org.jhotdraw.draw.io.InputFormat;
import org.jhotdraw.draw.io.OutputFormat;
import org.jhotdraw.draw.io.TextInputFormat;
import org.jhotdraw.xml.*;

/**
 * NetApplet.
 *
 *
 * @author Werner Randelshofer
 * @version $Id$
 */
public class NetApplet extends JApplet {

    private static final long serialVersionUID = 1L;
    private static final String NAME = "JHotDraw Net";
    private NetPanel drawingPanel;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup toolButtonGroup;
    // End of variables declaration//GEN-END:variables

    /**
     * We override getParameter() to make it work even if we have no Applet
     * context.
     */
    @Override
    public String getParameter(String name) {
        try {
            return super.getParameter(name);
        } catch (NullPointerException e) {
            return null;
        }
    }

    protected String getVersion() {
        return NetApplet.class.getPackage().getImplementationVersion();
    }

    /**
     * Initializes the applet NetApplet
     */
    @Override
    public void init() {
        // Set look and feel
        // -----------------
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Do nothing.
            // If we can't set the desired look and feel, UIManager does
            // automaticaly the right thing for us.
        }
        // Set our own popup factory, because the one that comes with Mac OS X
        // creates translucent popups which is not useful for color selection
        // using pop menus.
        try {
            PopupFactory.setSharedInstance(new PopupFactory());
        } catch (Exception e) {
            // If we can't set the popup factory, we have to use what is there.
        }
        // Display copyright info while we are loading the data
        // ----------------------------------------------------
        Container c = getContentPane();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        String[] labels = getAppletInfo().split("\n"); 
        for (int i = 0; i < labels.length; i++) {
            c.add(new JLabel((labels[i].length() == 0) ? " " : labels[i]));
        }
        // We load the data using a worker thread
        // --------------------------------------
        new SwingWorker<Drawing, Drawing>() {
            @Override
            protected Drawing doInBackground() throws Exception {
                Drawing result;
                System.out.println("getParameter.datafile:" + getParameter("datafile"));
                if (getParameter("data") != null) {
                    JavaxDOMInput domi = new JavaxDOMInput(new NetFactory(), new StringReader(getParameter("data")));
                    result = (Drawing) domi.readObject(0);
                } else if (getParameter("datafile") != null) {
                    URL url = new URL(getDocumentBase(), getParameter("datafile"));
                    InputStream in = url.openConnection().getInputStream();
                    try {
                        JavaxDOMInput domi = new JavaxDOMInput(new NetFactory(), in);
                        result = (Drawing) domi.readObject(0);
                    } finally {
                        in.close();
                    }
                } else {
                    result = null;
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    Drawing result = get();
                    Container c = getContentPane();
                    c.setLayout(new BorderLayout());
                    c.removeAll();
                    c.add(drawingPanel = new NetPanel());
                    if (result != null) {
                        Drawing drawing = result;
                        setDrawing(drawing);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(NetApplet.class.getName()).log(Level.SEVERE, null, ex);
                    failed(ex);
                }
                finished();
            }

            protected void failed(Throwable value) {
                Container c = getContentPane();
                c.setLayout(new BorderLayout());
                c.removeAll();
                c.add(drawingPanel = new NetPanel());
                value.printStackTrace();
                getDrawing().add(new TextFigure(value.toString()));
                value.printStackTrace();
            }

            protected void finished() {
                Container c = getContentPane();
                initDrawing(getDrawing());
                c.validate();
            }
        }.execute();
    }

    public void setData(String text) {
        if (text != null && text.length() > 0) {
            StringReader in = new StringReader(text);
            try {
                JavaxDOMInput domi = new JavaxDOMInput(new NetFactory(), in);
                domi.openElement("Net");
                setDrawing((Drawing) domi.readObject(0));
            } catch (Throwable e) {
                getDrawing().removeAllChildren();
                TextFigure tf = new TextFigure();
                tf.setText(e.getMessage());
                tf.setBounds(new Point2D.Double(10, 10), new Point2D.Double(100, 100));
                getDrawing().add(tf);
                e.printStackTrace();
            } finally {
                in.close();
            }
        }
    }

    public String getData() {
        CharArrayWriter out = new CharArrayWriter();
        try {
            JavaxDOMOutput domo = new JavaxDOMOutput(new NetFactory());
            domo.openElement("Net");
            domo.writeObject(getDrawing());
            domo.closeElement();
            domo.save(out);
        } catch (IOException e) {
            TextFigure tf = new TextFigure();
            tf.setText(e.getMessage());
            tf.setBounds(new Point2D.Double(10, 10), new Point2D.Double(100, 100));
            getDrawing().add(tf);
            e.printStackTrace();
        } finally {
            out.close();
        }
        return out.toString();
    }

    @Override
    public String[][] getParameterInfo() {
        return new String[][]{
            {"data", "String", "the data to be displayed by this applet."},
            {"datafile", "URL", "an URL to a file containing the data to be displayed by this applet."}};
    }

    @Override
    public String getAppletInfo() {
        return NAME
               + "\nVersion " + getVersion()
               + "\n\nCopyright 1996-2010 (c) by the original authors of JHotDraw and all its contributors"
               + "\nThis software is licensed under LGPL or"
               + "\nCreative Commons 3.0 BY";
    }
    

    private void setDrawing(Drawing d) {
        drawingPanel.setDrawing(d);
    }

    private Drawing getDrawing() {
        return drawingPanel.getDrawing();
    }

    /**
     * Configure Drawing object to support copy and paste.
     */
    @SuppressWarnings("unchecked")
    private void initDrawing(Drawing d) {
        d.setInputFormats((java.util.List<InputFormat>) Collections.EMPTY_LIST);
        d.setOutputFormats((java.util.List<OutputFormat>) Collections.EMPTY_LIST);
        DOMStorableInputOutputFormat ioFormat = new DOMStorableInputOutputFormat(
                new NetFactory());
        d.addInputFormat(ioFormat);
        d.addInputFormat(new ImageInputFormat(new ImageFigure()));
        d.addInputFormat(new TextInputFormat(new TextFigure()));
        d.addOutputFormat(ioFormat);
        d.addOutputFormat(new ImageOutputFormat());
    }

    /**
     * This method is called from within the init() method to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        toolButtonGroup = new javax.swing.ButtonGroup();
    }// </editor-fold>//GEN-END:initComponents
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame f = new JFrame("JHotDraw Net Applet");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                NetApplet a = new NetApplet();
                f.getContentPane().add(a);
                a.init();
                f.setSize(500, 400);
                f.setVisible(true);
                a.start();
            }
        });
    }

}
