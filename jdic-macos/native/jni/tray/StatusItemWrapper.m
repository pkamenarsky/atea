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
//  StatusItemWrapper.m
//
//  Created by Rob Ross on 9/29/06.


#import "StatusItemWrapper.h"


@implementation StatusItemWrapper : NSObject


- (id)init
{
	self = [super init];
	_customView = [[StatusItemView alloc] initWithFrame:NSMakeRect(0,0,22,22)]; //initially sized for an icon, no text
	_javaPeer = nil;

	return self;
}

- (void)installStatusItem;
{
	if (_statusItem == nil ) //we don't want to install more than one item
	{
		NSStatusBar *bar = [NSStatusBar systemStatusBar];
		
		_statusItem = [bar statusItemWithLength:NSVariableStatusItemLength];
		[_statusItem retain];
				
		[_statusItem setView:_customView];
		[_customView setStatusItem:_statusItem];
	}
}

- (void)removeStatusItem
{
	_running = NO; //stop any image animations
	
	if (_statusItem != nil)
	{
		NSStatusBar *bar = [NSStatusBar systemStatusBar];
		[bar removeStatusItem:_statusItem];			
		[_customView removeFromSuperview];
	}
	[_statusItem release];
	_statusItem = nil;
}

- (void)setImage:(NSImage*)theImage
{
	[_customView setImage:theImage];
}

- (NSImage*)image
{
	return [_customView image];
}

- (void)setLabelText:(NSString*)label
{
	[_customView setLabelText:label];
}

- (NSString*)labelText
{
	return [_customView labelText];	
}

- (void)setToolTip:(NSString*)toolTip
{
	[_customView setToolTip:toolTip];	
}

- (NSString*)toolTip
{
	return [_customView toolTip];
}

- (void)setIsArmed:(BOOL)armedState
{
    [_customView setIsArmed:armedState];
}

- (BOOL)isArmed
{
    return [_customView isArmed];
}

- (NSRect)globalFrame
{
    NSRect localFrame = [_customView frame];
    NSPoint globalOrigin = [_customView convertToGlobalPoint:localFrame.origin];


    NSRect globalFrame = NSMakeRect(globalOrigin.x, globalOrigin.y, localFrame.size.width, localFrame.size.height);

    return globalFrame;
}

- (void)setJavaPeer:(jobject) peer
{
    jweak weakRef = (*JNU_GetEnv())->NewWeakGlobalRef(JNU_GetEnv(), peer);
    if (weakRef == NULL)
    {
        NSLog(@"Out of memory creating week reference for _javaPeer");
        return; // out of memory
    }
    [_customView setJavaPeer:weakRef];
    _javaPeer = weakRef;
}

- (jobject)javaPeer
{
    return _javaPeer;
}

- (void)dealloc
{
	if (_statusItem != nil)
	{
		[self removeStatusItem];
	}
	
	if (_customView != nil)
	{
		[_customView release];
		_customView = nil;
	}

    (*JNU_GetEnv())->DeleteWeakGlobalRef(JNU_GetEnv(), _javaPeer);
	_javaPeer = NULL;

	[super dealloc];
}


@end
