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


static JavaVM *cached_jvm;

JNIEnv *JNU_GetEnv()
{
    JavaVM *jvm = NULL;
    JNIEnv *env = NULL;
    
    if (cached_jvm == NULL)
    {
        jsize bufLen = 1;
        jsize nVMs;
        jint vmError = JNI_GetCreatedJavaVMs(&jvm,bufLen, &nVMs);
        if (vmError == 0)
        {
            cached_jvm = jvm;
        }
    }
    
    if (cached_jvm == NULL)
    {
        NSLog(@"JavaVM is NULL");
    }
    else
    {
        (*cached_jvm)->GetEnv(cached_jvm,
                              (void **)&env,
                              JNI_VERSION_1_2);
    }
    return env;
}


@implementation StatusItemWrapper : NSObject


- (id)init
{
	self = [super init];
	_javaPeer = nil;

	return self;
}

- (void)installStatusItem;
{
	if (_statusItem == nil ) //we don't want to install more than one item
	{
		NSStatusBar *bar = [NSStatusBar systemStatusBar];
		
        _menu = [[NSMenu alloc] initWithTitle: @""];
        [_menu setDelegate:self];
        
		_statusItem = [bar statusItemWithLength:NSVariableStatusItemLength];
        [_statusItem setHighlightMode:YES];
        [_statusItem setMenu:_menu];
        
        [_statusItem retain];
	}
}

- (void)removeStatusItem
{
	if (_statusItem != nil)
	{
		NSStatusBar *bar = [NSStatusBar systemStatusBar];
		[bar removeStatusItem:_statusItem];			
	}
	[_statusItem release];
	_statusItem = nil;
}

- (void)setImage:(NSImage*)theImage
{
    [_statusItem setImage:theImage];
}

- (NSImage*)image
{
	return [_statusItem image];
}

- (NSMenu*)menu
{
    return _menu;
}

- (void)setLabelText:(NSString*)label
{
	[_statusItem setTitle:label];
}

- (NSString*)labelText
{
	return [_statusItem title];	
}

- (void)setToolTip:(NSString*)toolTip
{
	[_statusItem setToolTip:toolTip];	
}

- (NSString*)toolTip
{
	return [_statusItem toolTip];
}

- (void)menuWillOpen:(NSMenu *)menu
{
    static jmethodID mid;
    
    if (mid == NULL)
    {
        //first time initialization
        jclass serviceClass =
        (*JNU_GetEnv())->FindClass(JNU_GetEnv(), "org/jdesktop/jdic/tray/internal/impl/MacTrayIconService");
        if (serviceClass == NULL)
        {
            //error handling
            NSLog(@"serviceClass is null!, can't callback and notifiy MacTrayIconService about status item events.");
            return;
        }
        mid = (*JNU_GetEnv())->GetMethodID(JNU_GetEnv(), serviceClass, "statusItemSelectedCallback", "()V");
        if (mid == NULL)
        {
            //error handling
            NSLog(@"method ID for 'statusItemSelectedCallback' is null!, can't callback and notifiy MacTrayIconService about status item selected events.");
            return;
        }
    }
    
    if (mid != NULL)
    {
        if (_javaPeer != NULL)
        {
            if (JNI_FALSE == (*JNU_GetEnv())->IsSameObject(JNU_GetEnv(), _javaPeer, NULL))
            {
                //_javaPeer is non-null AND still holds a reference to a live object
                (*JNU_GetEnv())->CallVoidMethod(JNU_GetEnv(), _javaPeer, mid);
            }
        }
    }
}

- (void)itemSelected:(id)sender
{
    static jmethodID mid;
        
    if (mid == NULL)
    {
        //first time initialization
        jclass serviceClass =
        (*JNU_GetEnv())->FindClass(JNU_GetEnv(), "org/jdesktop/jdic/tray/internal/impl/MacTrayIconService");
        if (serviceClass == NULL)
        {
            //error handling
            NSLog(@"serviceClass is null!, can't callback and notifiy MacTrayIconService about item events.");
            return;
        }
        mid = (*JNU_GetEnv())->GetMethodID(JNU_GetEnv(), serviceClass, "itemSelectedCallback", "(I)V");
        if (mid == NULL)
        {
            //error handling
            NSLog(@"method ID for 'itemSelectedCallback' is null!, can't callback and notifiy MacTrayIconService about item selected events.");
            return;
        }
    }
    
    if (mid != NULL)
    {
        if (_javaPeer != NULL)
        {
            if (JNI_FALSE == (*JNU_GetEnv())->IsSameObject(JNU_GetEnv(), _javaPeer, NULL))
            {
                //_javaPeer is non-null AND still holds a reference to a live object
                jint index = [sender tag];
                (*JNU_GetEnv())->CallVoidMethod(JNU_GetEnv(), _javaPeer, mid, index);
            }
        }
    }
}

- (void)setJavaPeer:(jobject) peer
{
    jweak weakRef = (*JNU_GetEnv())->NewWeakGlobalRef(JNU_GetEnv(), peer);
    if (weakRef == NULL)
    {
        NSLog(@"Out of memory creating week reference for _javaPeer");
        return; // out of memory
    }
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
	
    (*JNU_GetEnv())->DeleteWeakGlobalRef(JNU_GetEnv(), _javaPeer);
	_javaPeer = NULL;

	[super dealloc];
}


@end
