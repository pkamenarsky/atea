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
/* Header for class org.jdesktop.jdic.tray.internal.impl.MacTrayIconService */

#ifndef _Included_MacTrayIconService
#define _Included_MacTrayIconService
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     _Included_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService
 * Method:    createStatusItem
 * Signature: ()J;
 */
JNIEXPORT jlong JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_createStatusItem
(JNIEnv *env, jobject this);


/*
 * Class:     _Included_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService
 * Method:    setImageNative
 * Signature: (J[BIIIIZZLjava/lang/String;II)V
 */
JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_setImageNative
(JNIEnv *env, jobject this, jlong nsStatusItemPtr, jbyteArray rasterData, jint imageWidth, jint imageHeight,
                            jint bitsPerSample, jint samplesPerPixel, jboolean hasAlpha, jboolean isPlanar,
                            jstring colorSpaceName, jint bytesPerRow, jint bitsPerPixel, jboolean isTemplate);


/*
 * Class:     _Included_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService
 * Method:    setTitleNative
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_setTitleNative
(JNIEnv *env, jobject this, jlong nsStatusItemPtr, jstring title);


/*
 * Class:     _Included_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService
 * Method:    setToolTipNative
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_setToolTipNative
(JNIEnv *env, jobject this, jlong nsStatusItemPtr, jstring);


/*
 * Class:     _Included_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService
 * Method:    removeStatusItem
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_removeStatusItem
(JNIEnv *env, jobject this, jlong nsStatusItemPtr);


JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_addItemNative
(JNIEnv *env, jobject this, jlong nsStatusItemPtr, jstring item, jint index, jint tag, jboolean enabled);


JNIEXPORT void JNICALL Java_org_jdesktop_jdic_tray_internal_impl_MacTrayIconService_removeItemNative
(JNIEnv *env, jobject this, jlong nsStatusItemPtr, jint index);
    
    
#ifdef __cplusplus
}
#endif
#endif
