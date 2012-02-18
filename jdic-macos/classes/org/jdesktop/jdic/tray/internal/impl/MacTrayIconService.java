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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

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
    private Icon icon;
	private boolean isTemplate;
    private String caption; //on the mac, this is the title(label) of the Status Item(Tray Icon)
    private String toolTipText;

	private int tag;
	private int itemCount;

	private ActionListener listener;

	private ArrayList<Integer> itemTags = new ArrayList<Integer>();
	private Map<Integer, ActionListener> listenerMap = new HashMap<Integer, ActionListener>();

    private long nsStatusItemWrapperPointer; //a C-pointer to the native NSStatusItemWrapper peer

    private final int MAC_STATUSBAR_ICON_WIDTH = 22; //the Status bar on the Mac is 22 points tall
    private final int MAC_STATUSBAR_ICON_HEIGHT = 22;

    private boolean nativePeerExists = false;

    private native long createStatusItem(); //creates, AND also adds the new StatusItem to the status bar

    private native void removeStatusItem(long nsStatusItemPtr); //removes item from status bar, which also frees all native resources

    private native void setImageNative(long nsStatusItemPtr, byte[] rasterData, int imageWidth, int imageHeight,
                                        int bitsPerSample, int samplesPerPixel, boolean hasAlpha, boolean isPlanar,
                                        String colorSpaceName, int bytesPerRow, int bitsPerPixel, boolean isTemplate);

    private native void setTitleNative(long nsStatusItemPtr, String title);

    private native void setToolTipNative(long nsStatusItemPtr, String toolTipText);

    private native void addItemNative(long nsStatusItemPtr, String item, int index, int tag, boolean enabled);
	
    private native void removeItemNative(long nsStatusItemPtr, int index);

	public MacTrayIconService(ActionListener listener)
	{
		this.listener = listener;
	}

	public MacTrayIconService()
	{
		this(null);
	}

	public void setActionListener(ActionListener listener)
	{
		this.listener = listener;
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
			icon.paintIcon(null, g, 0, 0);
			
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
						isPlanar, colorSpaceName, bytesPerRow, bitsPerPixel, isTemplate);
			}
        }
    }

    public void setIcon(final Icon i, final boolean isTemplate) {

		if (i == null)
			throw new IllegalArgumentException("icon == null"); 

		icon = i;
		this.isTemplate = isTemplate;

		updateIcon();
	}

    public void setIcon(final Icon i) {
		setIcon(i, true);
	}

    /**
	 * Set the label of the tray icon (title of the Status Item)
	 * 
	 * @param labelText
	 *            text to display as the Tray Icon's label
	 */
    public void setCaption(String labelText)
    {
		if (labelText == null)
			throw new IllegalArgumentException("caption == null");

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

	public void addItem(String item, int index, ActionListener listener)
	{
		if (item == null)
			throw new IllegalArgumentException("item == null");

		if (nativePeerExists)
		{
			if (index <= itemTags.size())
			{
				if (index == itemTags.size()) {
					itemCount = itemTags.size() + 1;
				}

				itemTags.add(index, tag);
				listenerMap.put(tag, listener);

				addItemNative(nsStatusItemWrapperPointer, item, index, tag++, listener != null);
			}
			else
				throw new IllegalArgumentException("Illegal index " + index);
		}
	}

	public void addItem(String item, ActionListener listener)
	{
		addItem(item, itemCount, listener);
	}

	public void addSeparator(int index)
	{
		addItem("-", index, null);
	}

	public void addSeparator() {
		addSeparator(itemCount);
	}

	public void removeItem(int index)
	{
		if (nativePeerExists)
		{
			if (index < itemTags.size()) {
				removeItemNative(nsStatusItemWrapperPointer, index);

				listenerMap.remove(itemTags.get(index));
				itemTags.remove(index);

				itemCount--;
			}
			else
				throw new IllegalArgumentException("No menu item at index " + index);
		}
	}

	public int getItemCount()
	{
		return itemCount;
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
		if (nativePeerExists) {
			// remove native status item from native status bar
			removeStatusItem(nsStatusItemWrapperPointer);
			nsStatusItemWrapperPointer = 0;
			nativePeerExists = false;
		}
	}

	void statusItemSelectedCallback()
	{
		if (listener != null)
		{
			listener.actionPerformed(new ActionEvent(this, 0, "statusItemSelected"));
		}
	}

	void itemSelectedCallback(int tag)
	{
		if (listenerMap.containsKey(tag))
		{
			listenerMap.get(tag).actionPerformed(new ActionEvent(this, tag, "itemSelected"));
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
