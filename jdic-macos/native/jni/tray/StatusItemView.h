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
//  StatusItemView.h
//
//  Created by Rob Ross on 9/29/06.

#import <jni.h>
#import <Cocoa/Cocoa.h>

JNIEnv *JNU_GetEnv();

@interface StatusItemView : NSView {
	
	NSImage *image;
	NSString *labelText;
	NSImageView *imageView; 
	
	NSStatusItem *_statusItem;
	NSRect _imageRect; //frame size of icon view drawn within this parent StatusItemView

	jobject _javaPeer; //this is not retained here; it is retained by the StatusItemWrapper that owns us
	BOOL     isArmed; //YES when the status item is 'armed', ie, highlighted in response to a user-click.

}

- (void)setStatusItem:(NSStatusItem *)statusItem;
- (void)setImage:(NSImage*)theImage;
- (NSImage*)image;
- (void)setLabelText:(NSString*)label;
- (NSString*)labelText;
- (void)setIsArmed:(BOOL)armedState;
- (BOOL)isArmed;
- (NSPoint)convertToGlobalPoint:(NSPoint) localPoint;

- (void)setJavaPeer:(jobject) peer;
- (jobject)javaPeer;

@end


@interface CustomImageView : NSImageView
{
}

@end