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

#import <jni.h>
#import <Cocoa/Cocoa.h>
#import <inttypes.h>
#import "MacTrayIconService.h"
#import "ConversionUtils.h"
#import "StatusItemWrapper.h"


JNIEXPORT jlong JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_createStatusItem
(JNIEnv *env, jobject this)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    //NSLog(@"Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_createStatusItem");

    StatusItemWrapper *theItem = [[StatusItemWrapper alloc] init];

    [theItem setJavaPeer:this];
    [theItem installStatusItem];

    [pool release];

    return  (uintptr_t)theItem;
}


JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_setTitleNative
(JNIEnv *env, jobject this, jlong nsStatusItemPtr, jstring title)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    //NSLog(@"In the native setTitleNative method");
    StatusItemWrapper *theItem;
    theItem = (id) ((uintptr_t)nsStatusItemPtr);

    NSString *text = ConvertToNSString(env, title);

    [theItem setLabelText:text];

    [pool release];
}


JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_setToolTipNative
(JNIEnv *env, jobject this, jlong nsStatusItemPtr, jstring toolTipText)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    StatusItemWrapper *theItem;
    theItem = (id) ((uintptr_t)nsStatusItemPtr);

    NSString *text = ConvertToNSString(env, toolTipText);
    [theItem setToolTip:text];

    [pool release];
}

JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_removeStatusItem
(JNIEnv *env, jobject this, jlong nsStatusItemPtr)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    StatusItemWrapper *theItem;
    theItem = (id) ((uintptr_t)nsStatusItemPtr);
    [theItem removeStatusItem];

    //NSLog(@"Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_removeStatusItem");

    [pool release];
}


JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_addItemNative
(JNIEnv *env, jobject this, jlong nsStatusItemPtr, jstring item, jint index, jint tag, jboolean enabled)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    
    //NSLog(@"In the native setTitleNative method");
    StatusItemWrapper *theItem;
    theItem = (id) ((uintptr_t)nsStatusItemPtr);

    NSString *nsItem = ConvertToNSString(env, item);
    NSMenuItem *nsMenuItem = [nsItem isEqualToString:@"-"] ?
        [NSMenuItem separatorItem] :
        [[NSMenuItem alloc] initWithTitle:nsItem action:@selector(itemSelected:) keyEquivalent:@""];
    
    if (enabled)
    {
        [nsMenuItem setTarget:theItem];
        [nsMenuItem setTag:tag];
    }
            
    [[theItem menu] insertItem:nsMenuItem atIndex:index];

    [pool release];
}


JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_removeItemNative
(JNIEnv *env, jobject this, jlong nsStatusItemPtr, jint index)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    
    //NSLog(@"In the native setTitleNative method");
    StatusItemWrapper *theItem;
    theItem = (id) ((uintptr_t)nsStatusItemPtr);
    
    [[theItem menu] removeItemAtIndex:index];
    [pool release];
}


JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_setImageNative
(JNIEnv *env, jobject this, jlong nsStatusItemPtr, jbyteArray rasterData, jint imageWidth, jint imageHeight,
                            jint bitsPerSample, jint samplesPerPixel, jboolean hasAlpha, jboolean isPlanar,
                            jstring colorSpaceName, jint bytesPerRow, jint bitsPerPixel, jboolean isTemplate)
{
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        StatusItemWrapper *theItem;
        theItem = (id) ((uintptr_t)nsStatusItemPtr);

        jint len = (*env)->GetArrayLength(env, rasterData);
        jbyte carr[len];
        (*env)->GetByteArrayRegion(env, rasterData, 0, len, carr);


        if (carr == NULL)
        {
            return; // exception occurred
        }
        unsigned char *bufPtr = (unsigned char *)carr;

        BOOL alpha = (hasAlpha == 0 ? NO : YES);
        BOOL planar = (isPlanar == 0 ? NO : YES);
        NSString *colorSpace = ConvertToNSString(env, colorSpaceName);


       // NSLog(@"setImageNative3: imageWidth=%i, imageHeight=%i, bitsPerSample=%i, samplesPerPixel=%i, alpha=%i, planar=%i, colorSpace=%@, bytesPerRow=%i, bitsPerPixel=%i", imageWidth,imageHeight, bitsPerSample, samplesPerPixel,alpha,planar,colorSpace,bytesPerRow,bitsPerPixel );

        //Make an NSImage from the data
        NSBitmapImageRep *bmrep = [NSBitmapImageRep alloc ];
        bmrep = [bmrep initWithBitmapDataPlanes:NULL
        pixelsWide:imageWidth
        pixelsHigh:imageHeight
        bitsPerSample:bitsPerSample
        samplesPerPixel:samplesPerPixel
        hasAlpha:alpha
        isPlanar:planar
        colorSpaceName:colorSpace
        bytesPerRow:bytesPerRow
        bitsPerPixel:bitsPerPixel];

        //copy the pixel data into the buffer manged by the NSBitmapImageRep
        unsigned char *dataPtr = [bmrep bitmapData];
        for (int i = 0, n = (bytesPerRow *imageHeight) ; i < n; i++)
        {
            dataPtr[i] = bufPtr[i];
        }

        [bmrep autorelease];

        NSImage *bmi = [[NSImage alloc] init];
        [bmi autorelease];

        [bmi addRepresentation:bmrep];
        [bmi setTemplate:isTemplate];

        if ([bmi isValid])
        {
        [theItem performSelectorOnMainThread:@selector(setImage:) withObject:bmi waitUntilDone:NO];  
        }
        else
        {
            NSLog(@"Image is NOT valid");
        }

        [pool release];
}