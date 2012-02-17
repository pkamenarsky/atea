/*
 * Copyright (C) 2004 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */


package org.jdesktop.jdic.tray.internal.impl;

/**
 *  The <code>MacTrayIconService</code> is the delegate class for native Mac
 *  <code>TrayIcon</code> implementation.
 *  @author Rob Ross <robross@earthlink.net>
 */

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * The <code>MacTrayIconService</code> is the delegate class for native Mac
 * <code>TrayIcon</code> implementation. 
 * Note : Do not override equals()! The MacSystemTrayService keeps track of 
 * installed TrayIcon instances by placing their MacTrayIconService delegates 
 * in a Set and relies on comparisons by object identity. Overriding equals here 
 * may cause unpredictable results when trying to add or remove tray icons.
 * @author Rob Ross <robross@earthlink.net>
 */

public class MacTrayIconService 
{

    private JPopupMenu menu;
    private Icon icon;
    private boolean autoSize;
    private String caption; //on the mac, this is the title(label) of the Status Item(Tray Icon)
    private String toolTipText;

    private long nsStatusItemWrapperPointer; //a C-pointer to the native NSStatusItemWrapper peer
    static private HashMap map = new HashMap();

    private LinkedList actionList = new LinkedList();

    private final int MAC_STATUSBAR_ICON_WIDTH = 22; //the Status bar on the Mac is 22 points tall
    private final int MAC_STATUSBAR_ICON_HEIGHT = 22;

    private boolean nativePeerExists = false;
    private boolean showingPopoup;

    AnimationObserver observer;

    static class PopupParent extends JDialog
    {
        public PopupParent()
        {
            super((Frame) null, "JDIC Tray Icon");
            try
            {
                Method setAlwaysOnTop = this.getClass().getMethod("setAlwaysOnTop", new Class[]{boolean.class});
                setAlwaysOnTop.invoke(this, new Object[]{Boolean.TRUE});
            }
            catch (NoSuchMethodException e)
            {
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            this.setUndecorated(true);
            this.setBounds(0, 0, 0, 0);
        }
    }

    static PopupParent popupParentFrame;


    public MacTrayIconService()
    {
        if (popupParentFrame == null)
        {
            popupParentFrame = new PopupParent();
            popupParentFrame.pack();
            popupParentFrame.setVisible(true);
        }
    }


    private native long createStatusItem(); //creates, AND also adds the new StatusItem to the status bar

    private native void removeStatusItem(long nsStatusItemPtr); //removes item from status bar, which also frees all native resources

    private native void setImageNative(long nsStatusItemPtr, byte[] rasterData, int imageWidth, int imageHeight,
                                        int bitsPerSample, int samplesPerPixel, boolean hasAlpha, boolean isPlanar,
                                        String colorSpaceName, int bytesPerRow, int bitsPerPixel);

    private native void setTitleNative(long nsStatusItemPtr, String title);

    private native void setToolTipNative(long nsStatusItemPtr, String toolTipText);

    private native void setIsArmedNative(long nsStatusItemPtr, boolean state);//control highlight of item in status bar


    /**
     * @param nsStatusItemPtr
     * @param framePoints     a 4 element float array. After the call returns, the array contains the frame rect points of
     *                        the StatusItem's view (TrayIcon view) as originX, originY, width, height, in global screen coordinates
     */
    private native void getLocationOnScreenNative(long nsStatusItemPtr, float[] framePoints);


    /**
     * Not implementd on the Mac
     *
     * @param caption
     * @param text
     * @param type
     */
    public void showBalloonMessage(String caption, String text, int type)
    {
        //this is currently a no-op on the Mac
    	throw new UnsupportedOperationException("Method showBalloonMessage is unsupported under Mac!");
    }

    public void addNotify()
    {
        //we can't call the native methods on the AWT thread since deadlocks can occur
        if (SwingUtilities.isEventDispatchThread())
        {
            Runnable r = new Runnable()
            {
                public void run()
                {
                    addImpl();
                }
            };
            new Thread(r).start();
        }
        else
        {
            addImpl();
        }
    }

    private synchronized void addImpl()
    {
        if (! nativePeerExists) //we only want to add this tray icon to the Status Bar once
        {
            //initialize all needed native resources
            nsStatusItemWrapperPointer = createStatusItem();
     
            setTitleNative(nsStatusItemWrapperPointer, caption);
            setToolTipNative(nsStatusItemWrapperPointer, toolTipText);

            nativePeerExists = true;
            updateIcon();
        }
    }


    void updateIcon()
    {
        if (icon != null)
        {
            Graphics2D g;
			BufferedImage iconImage = new BufferedImage(icon.getIconWidth(),
					icon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			g = (Graphics2D) ((BufferedImage) iconImage).getGraphics();
			g.setComposite(AlphaComposite.Src);
			icon.paintIcon(observer, g, 0, 0);
			
            WritableRaster wr = iconImage.getRaster();
            DataBuffer db = wr.getDataBuffer();

            byte[] pixels = ((DataBufferByte) db).getData(); //this is stored as ABGR, but we want RGBA so we need to reverse the byte order
            for (int i = 0, n = pixels.length; i < n; i += 4)
            {
				byte alpha = pixels[i];
				byte blue = pixels[i + 1];
				byte green = pixels[i + 2];
				byte red = pixels[i + 3];

				pixels[i] = red;
				pixels[i + 1] = green;
				pixels[i + 2] = blue;
				pixels[i + 3] = alpha;
			}

            int imageWidth = wr.getWidth();
            int imageHeight = wr.getHeight();
            int samplesPerPixel = wr.getNumBands();
            int bitsPerSample = 8;
            boolean hasAlpha = true;
            boolean isPlanar = false;
            String colorSpaceName = "NSCalibratedRGBColorSpace"; //if doesn't work try NSDeviceRGBColorSpace or NSCalibratedRGBColorSpace
            int bytesPerRow = imageWidth * samplesPerPixel;
            int bitsPerPixel = samplesPerPixel * bitsPerSample;
			if (nativePeerExists) {
				setImageNative(nsStatusItemWrapperPointer, pixels, imageWidth,
						imageHeight, bitsPerSample, samplesPerPixel, hasAlpha,
						isPlanar, colorSpaceName, bytesPerRow, bitsPerPixel);
			}
        }
    }


    private void temp()
    {

        // here is the data I need to supply to create an NSBitmapImageRep in
		// the native Mac code:
        /*
		 * bmrep = [bmrep initWithBitmapDataPlanes:&image->buffer_ptr
		 * pixelsWide:(GLint) image->buffer_size.width pixelsHigh:(GLint)
		 * image->buffer_size.height bitsPerSample:8 samplesPerPixel:4
		 * hasAlpha:YES isPlanar:NO colorSpaceName:NSCalibratedRGBColorSpace
		 * bytesPerRow:image->buffer_size.width*4 bitsPerPixel:32];
		 * 
		 * assume we can start from a BufferedImage named bi, into which our
		 * Icon has been drawn. planes - the data buffer pixelsWide - width of
		 * data in pixels pixelsHigh - height of data in pixels
		 */
        Graphics2D g;
        Image iconImage = null;
        if (icon != null)
        {
            if (iconImage == null)
            {
                iconImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                g = (Graphics2D) ((BufferedImage) iconImage).getGraphics();
                g.setComposite(AlphaComposite.Src);
                icon.paintIcon(null, g, 0, 0);
                BufferedImage bi = (BufferedImage) iconImage; //for casting
                System.out.println("type of BufferedImage is " + iconImage.getClass().getName());
                System.out.println("width of image is " + bi.getWidth());
                System.out.println("height of image is " + bi.getHeight());

                Raster r = bi.getData();
                System.out.println("bi.getDate() : type of the Raster is " + r.getClass().getName());
                DataBuffer db = r.getDataBuffer();
                System.out.println("type of the DataBuffer is " + db.getClass().getName());
                int pixels[] = ((DataBufferInt) db).getData();
                System.out.println("size of pixel array is " + pixels.length);

                System.out.println("");
                WritableRaster wr = bi.getRaster();
                System.out.println("bi.getRaster() : type of the Raster is " + wr.getClass().getName());
                DataBuffer db2 = wr.getDataBuffer();
                System.out.println("type of DataBuffer is " + db2.getClass().getName());

                pixels = ((DataBufferInt) db2).getData();
                System.out.println("size of pixel array is " + pixels.length);
                System.out.println("raster bouds is " + wr.getBounds());
                System.out.println("raster width=" + wr.getWidth() + ", height=" + wr.getHeight());
                System.out.println("raster minX=" + wr.getMinX() + ", minY=" + wr.getMinY());
                System.out.println("raster number bands (samples per pixel)=" + wr.getNumBands());
                System.out.println("raster num data elements =" + wr.getNumDataElements());
                System.out.println("");
                int imageWidth = wr.getWidth();
                int imageHeight = wr.getHeight();
                int samplesPerPixel = wr.getNumBands();
                int bitsPerSample = 8;
                boolean hasAlpha = true;
                boolean isPlanar = false;
                String colorSpaceName = "NSCalibratedRGBColorSpace"; //if doesn't work try NSDeviceRGBColorSpace
                int bytesPerRow = imageWidth * samplesPerPixel;
                int bitsPerPixel = samplesPerPixel * bitsPerSample;

                SampleModel sm = bi.getSampleModel();
                System.out.println("type of the SampleModel is " + sm.getClass().getName());
                int[] sampleSize = sm.getSampleSize();
                System.out.println("length of sample size array is " + sampleSize.length);
                System.out.println("samples are :");
                for (int i = 0; i < sampleSize.length; i++)
                {
                    int i1 = sampleSize[i];
                    System.out.print("" + i + ": " + i1 + ", ");
                }
                System.out.println("");
                if (sm.getClass() == SinglePixelPackedSampleModel.class)
                {
                    System.out.println("scanline stride is " + ((SinglePixelPackedSampleModel) sm).getScanlineStride());
                }

            }
        }
    }



    //sets showingPopup to state, and calls native peer to set the state of the TrayIcon to be highlighted or unhighlighted
    private void setIsArmed(boolean state)
    {
        showingPopoup = state;
        if (nativePeerExists)
        {
            setIsArmedNative(nsStatusItemWrapperPointer, state);
        }
    }

    public void setPopupMenu(JPopupMenu m)
    {
        menu = m;
        if (menu != null)
        {
            menu.setLightWeightPopupEnabled(false);

            menu.addPopupMenuListener(new PopupMenuListener()
            {
                public void popupMenuWillBecomeVisible(PopupMenuEvent e)
                {
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
                {
                    setIsArmed(false);
                }

                public void popupMenuCanceled(PopupMenuEvent e)
                {
                    setIsArmed(false);
                }
            });

            // in jdk1.4, the popup menu is still visible after the invoker window lost focus.
            popupParentFrame.addWindowFocusListener(new WindowFocusListener()
            {
                public void windowGainedFocus(WindowEvent e)
                {
                }

                public void windowLostFocus(WindowEvent e)
                {
                    menu.setVisible(false);

                }
            });

        }
    }

    public void processEvent(int mouseState, int x, int y)
    {

    }

    public synchronized static void notifyEvent(int id, final int mouseState, final int x, final int y)
    {
        final MacTrayIconService instance = (MacTrayIconService) map.get(new Integer(id));
        if (instance == null)
        {
            return;
        }
        try
        {
            EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    instance.processEvent(mouseState, x, y);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public void setIcon(final Icon i) {
		icon = i;
		if (observer != null) {
			observer.setUpdate(false);
			observer = null;
		}
		observer = new AnimationObserver();
		if (icon instanceof ImageIcon) {
			observer = new AnimationObserver();
			((ImageIcon) icon).setImageObserver(observer);
		}
		updateIcon();
	}

    /**
	 * Set the label of the tray icon (title of the Status Item)
	 * 
	 * @param labelText
	 *            text to display as the Tray Icon's label
	 */
    public void setCaption(String labelText)
    {
        caption = labelText;
        if (nativePeerExists)
        {
            setTitleNative(nsStatusItemWrapperPointer, caption);
        }
    }

    /**
     * Set the toolTip of the tray Icon (Status Item)
     *
     * @param toolTip the text to display as the mouse overs over the tray icon
     */
    public void setToolTip(String toolTip)
    {
        toolTipText = toolTip;
        if (nativePeerExists)
        {
            setToolTipNative(nsStatusItemWrapperPointer, toolTipText);
        }
    }

    public void setIconAutoSize(boolean b)
    {
        autoSize = b;

    }

    public void addActionListener(ActionListener l)
    {
        actionList.add(l);
    }

    public void removeActionListener(ActionListener l)
    {
        actionList.remove(l);
    }

    public Point getLocationOnScreen()
    {
        Point location = null;
        if (nativePeerExists)
        {
            float[] buf = new float[4];
            //native method copies in originX, originY, width, height into buf
            getLocationOnScreenNative(nsStatusItemWrapperPointer, buf);
            location = new Point((int) buf[0], (int) buf[1]);
            //System.out.println("getLocationOnScreen: x="+buf[0]+", y="+buf[1]+", width="+buf[2]+", height="+buf[3]);
        }
        return location;
    }

    void remove()
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            Runnable r = new Runnable()
            {
                public void run()
                {
                    removeImpl();
                }
            };
            new Thread(r).start();
        }
        else
        {
            removeImpl();
        }

    }

    private synchronized void removeImpl() {
		if (observer != null) {
			observer.setUpdate(false);
		}
		if (nativePeerExists) {
			// remove native status item from native status bar
			removeStatusItem(nsStatusItemWrapperPointer);
			nsStatusItemWrapperPointer = 0;
			nativePeerExists = false;
		}
	}


    public void addBalloonActionListener(ActionListener al)
    {

    }

    public void removeBalloonActionListener(ActionListener al)
    {

    }


    /**
     * Called from JNI when a mouse event has occured in the TrayIcon. This will be on the AWT-AppKit thread, NOT
     * the EDT, so when procesing from this method make sure any events you sent posted are on the EDT thread otherwise a
     * deadlock may occurr
     *
     * @param eventName
     * @param mouseX    the x location of the mosue event in global screen coordinates
     * @param mouseY    the y location of the mouse event in global screen coordinates
     * @param itemX     the x-coordinate of the NSStatusItem's view, in global screen coordinates.
     */
    void mouseEventCallback(String eventName, final float mouseX, final float mouseY, final float itemX)
    {
        if ("rightMouseDown".equals(eventName) || "mouseDown".equals(eventName))
        {
            if (menu != null)
            {
                if (showingPopoup)
                {
                    //if already showing the popup, toggle the state, ie, hide it
                    Runnable r = new Runnable()
                    {
                        public void run()
                        {
                            menu.setVisible(false);
                        }
                    };
                    SwingUtilities.invokeLater(r);
                    return;
                }

                if (!actionList.isEmpty() && "mouseDown".equals(eventName))
                {
                    //there are registered action listeners for the mouse-down. So instead of showing the popup, we
                    //notify the listeners
                    Runnable r = new Runnable()
                    {
                        public void run()
                        {
                            Iterator itar = actionList.iterator();
                            while (itar.hasNext())
                            {
                                ActionListener al = (ActionListener) itar.next();
                                al.actionPerformed(new ActionEvent(MacTrayIconService.this,
                                                                   ActionEvent.ACTION_PERFORMED, "PressAction"));
                            }
                        }
                    };

                    SwingUtilities.invokeLater(r);
                    return;
                }

                //final Point p = new Point((int)mouseX, (int)mouseY);
                Runnable r = new Runnable()
                {
                    public void run()
                    {
                        popupParentFrame.pack();
                        popupParentFrame.setVisible(true);
                        menu.show(popupParentFrame.getContentPane(), (int) itemX, MAC_STATUSBAR_ICON_HEIGHT);
                        menu.requestFocus();
                        popupParentFrame.toFront();
                        setIsArmed(true);
                    }
                };
                SwingUtilities.invokeLater(r);
            }
        }
    }

    private class AnimationObserver extends Component implements ImageObserver {
		boolean update = true;

		public void setUpdate(boolean b) {
			update = b;
		}

		public boolean imageUpdate(Image img, int infoflags, int x, int y,
				int width, int height) {
			if (update
					&& (((infoflags & ALLBITS) != 0) || ((infoflags & FRAMEBITS) != 0))) {
				updateIcon();
			}
			return update;
		}
	}


    // test method
    private void showThreads()
    {
        Thread[] threads = new Thread[Thread.activeCount()];
        int size = Thread.enumerate(threads);
        for (int i = 0; i < threads.length; i++)
        {
            Thread thread = threads[i];
            System.out.println("thread " + i + "+: " + thread.getName());
        }

        System.out.println("Current thread is : " + Thread.currentThread().getName());
        if (SwingUtilities.isEventDispatchThread())
        {
            System.out.println("You're in the EVENT DISPATCH thread buddy!!");
        }
        else
        {
            System.out.println("NOT the Event-dispatch thread!");
        }
    }

    /**
     * Free native resources associated with this instance.
     */
    synchronized void dispose()
    {
        remove();

    }

    protected void finalize() throws Throwable
    {
        dispose();
        super.finalize();
    }

}
