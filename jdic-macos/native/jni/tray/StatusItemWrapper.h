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

//
//  StatusItemWrapper.h
//
//  Created by Rob Ross on 9/29/06.

//
// This class manages the state for a single instance of an NSStatusItem, including its custom NSView
//

#import <jni.h>
#import <Cocoa/Cocoa.h>



@interface StatusItemWrapper : NSObject <NSMenuDelegate> {

	NSStatusItem *_statusItem;
    NSMenu *_menu;

	jobject _javaPeer; //if not nil, holds global reference to the Java object that is managing this native instance in the JVM
}

- (void)installStatusItem;
- (void)removeStatusItem;

- (NSMenu*)menu;
- (void)setImage:(NSImage*)theImage;
- (NSImage*)image;
- (void)setLabelText:(NSString*)label;
- (NSString*)labelText;
- (void)setToolTip:(NSString*)toolTip;
- (NSString*)toolTip;

- (void)setJavaPeer:(jobject) peer;
- (jobject)javaPeer;

- (void)itemSelected:(id) sender;

@end

