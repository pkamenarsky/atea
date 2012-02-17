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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * The <code>MacSystemTrayService</code> is the Mac OS implementation of the
 * <code>SystemTrayService</code> interface. There is only one Status Bar on
 * the Mac, and this Java class represents that native peer, so this is a
 * singleton. Use the factory method getInstance to get the single instance of
 * this class.
 * 
 * @author Rob Ross <robross@earthlink.net>
 */
public class MacSystemTrayService {

    private static MacSystemTrayService instance;

    /**
     * Contains all the Status Items currently displayed in the Status Bar.
     * When a Status Item (tray icon) is removed, the native resources it holds are freed; but the
     * owner of the tray icon may add the tray icon again and those native resources will be
     * re-allocated.
     */
    private Set trayIcons = new HashSet();


    private MacSystemTrayService()
    {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                dispose();
            }
        });
    }

    public static MacSystemTrayService getInstance()
    {
        if (instance == null)
        {
            instance = new MacSystemTrayService();
        }
        return instance;
    }

    public void addNotify() {}
  

    public synchronized void addTrayIcon(MacTrayIconService trayIcon, int trayIndex)
    {
        if (! trayIcons.contains(trayIcon))
        {
            trayIcons.add(trayIcon);
            trayIcon.addNotify();
        }
    }

    public synchronized void removeTrayIcon(MacTrayIconService trayIcon, int trayIndex)
    {
        if (trayIcons.contains(trayIcon))
        {
            trayIcons.remove(trayIcon);
            trayIcon.remove(); //removes from Status Bar and frees native resources
        }
    }

    /**
     * Removes all tray icons currently attached to the Status Bar (system tray), and frees
     * their native resources
     */
    private synchronized void dispose()
    {
        if (! trayIcons.isEmpty())
        {
            Iterator itar = trayIcons.iterator();
            while (itar.hasNext())
            {
                MacTrayIconService trayIcon = (MacTrayIconService) itar.next();
                trayIcon.dispose();
            }
            trayIcons.clear();
        }
    }

    protected void finalize() throws Throwable
    {
        dispose();
        super.finalize();
    }

    static
    {
        System.loadLibrary("tray");   //filesystem name is libtray.jnilib
    }
}
