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


/*
 * ConversionUtils.h
 *
 * header file for ConversionUtils.m
 * Contains various utility methods helpful from converting from/to Mac native data and Java data
 *
 */


#import <Cocoa/Cocoa.h>
#import <jni.h>

#ifndef _ConversionUtils
#define _ConversionUtils


 /*
 * ConvertToNSString
 *
 * given a non-null jstring argument, return the equivalent NSString representation. The object is autoreleased.
 *
 * This function returns NULL if the argument is NULL, or if the NSString couldn't be created. Requires the JNIEnv
 * to be passed as the first argument
 *
 */
 NSString *ConvertToNSString(JNIEnv *env, jstring str);

 /*
 * ConvertToCFStringRef
 *
 * given a non-null jstring argument, return the equivalent CFStringRef representation. The object is retained.
 *
 * This function returns NULL if the argument is NULL, or if the CFStringRef couldn't be created. Requires the JNIEnv
 * to be passed as the first argument
 *
 */
 CFStringRef ConvertToCFStringRef(JNIEnv *env, jstring str);


/*
 * CreateJavaStringFromNSString
 *
 * given a non-null NSString argument, return the equivalent Java String representation.
 *
 * This function returns NULL if the argument is NULL, or if the jstring couldn't be created. Requires the JNIEnv
 * to be passed as the first argument
 *
 */
jstring CreateJavaStringFromNSString(JNIEnv *env, NSString *nativeStr);


/*
 * CreateJavaStringFromCFStringRef
 *
 * given a non-null CFStringRef argument, return the equivalent Java String representation.
 *
 * This function returns NULL if the argument is NULL, or if the jstring couldn't be created. Requires the JNIEnv
 * to be passed as the first argument
 *
 */
jstring CreateJavaStringFromCFStringRef(JNIEnv *env, CFStringRef nativeStr);

#endif