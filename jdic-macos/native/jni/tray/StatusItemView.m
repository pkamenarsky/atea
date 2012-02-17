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
//  StatusItemView.m
//
//  Created by Rob Ross on 9/29/06.

#import "StatusItemView.h"
#import "ConversionUtils.h"

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


//subclassing NSImageView so we can get notified when the right-mouse button events occur

@implementation CustomImageView : NSImageView

- (id)initWithFrame:(NSRect)frameRect
{
	self =  [super initWithFrame:frameRect];
	return self;
}

- (void)rightMouseDown:(NSEvent *)theEvent
{
	//let the superview (the StatusItemView) handle this
	[[self superview] rightMouseDown:theEvent];
}

- (void)rightMouseUp:(NSEvent *)theEvent
{
	//let the superview (the StatusItemView) handle this
	[[self superview] rightMouseUp:theEvent];
}

@end

@implementation StatusItemView : NSView

- (id)initWithFrame:(NSRect)frameRect
{
	self = [super initWithFrame:frameRect];
	_imageRect = NSMakeRect(2,1,23,23);
	//add an image view to ourself
	imageView = [[CustomImageView alloc] initWithFrame:_imageRect];
	//[imageView setAnimates:YES];
	[imageView setImageScaling:NSScaleProportionally];
	[imageView setImageFrameStyle:NSImageFrameNone];
	
	[imageView setEnabled:NO];

	[self addSubview:imageView];

	_javaPeer = nil;
	return self;
}

- (void)setStatusItem:(NSStatusItem *)statusItem
{
	_statusItem = statusItem; //not retained to avoid a retain cycle	
}

// convenience method that calls setNeedsDisplay on the main app thread (AppKit thread)

- (void)updateDisplay
{
   [self performSelectorOnMainThread:@selector(setNeedsDisplay:) withObject:[NSNumber numberWithBool:YES] waitUntilDone:NO];
}

//updates the size of the status item's view, based on the width of the Image and Label

- (void)resize
{
	float width = 0.0;
	if (image != nil)
	{
		width = 24.0; //allocating space for the image
	}
	if (labelText != nil)
	{
		//allocate size for the text
		NSFont *font = [NSFont menuBarFontOfSize:0];
		NSDictionary *dict = [NSDictionary dictionaryWithObject:font forKey:NSFontAttributeName];
		NSAttributedString *str = [[NSAttributedString alloc] initWithString:labelText attributes:dict];
		[str autorelease];
		
		NSSize strSize = [str size];
		//NSLog(@"width of string=%@ : width=%f, height=%f",[str string], strSize.width, strSize.height);

		width += (strSize.width + 1.0);
	}
	width += 2.0; //for inset between edge of view and image view	
	[self setFrameSize:NSMakeSize(width, 22.0)];
	//NSLog(@"resized width of view to %f", width);

	[self updateDisplay];
}

- (void)setImage:(NSImage*)theImage
{
	[theImage retain];
	[image release];
	image = theImage;
	[imageView setImage:image];
	if (image == nil)
	{
			//no image, so resize the image view
			NSLog(@"setImage in StatusItemview - image is nil");
		[imageView setFrame:NSMakeRect(0,0,0,0)];
	}
	else
	{
		[imageView setFrame:_imageRect];
	}
    [imageView setNeedsDisplay:YES];
	[self resize];
}

- (NSImage*)image
{
	return image;
}

- (void)setLabelText:(NSString*)label
{
	[label retain];
	[labelText release];
	labelText = label;
	[self resize];
}

- (NSString*)labelText
{
	return labelText;
}


- (NSPoint)convertToGlobalPoint:(NSPoint) localPoint
{
   return [[self window] convertBaseToScreen:localPoint];
}


- (void)doCallBackToJVM:(jstring)eventType pointInWindow:(NSPoint)localPoint
{
   static jmethodID mid;

   //first bring the application to the front
   [NSApp activateIgnoringOtherApps:YES];

   if (mid == NULL)
   {
        //first time initialization
        jclass serviceClass =
        (*JNU_GetEnv())->FindClass(JNU_GetEnv(), "org/jdesktop/jdic/tray/internal/impl/MacTrayIconService");
        if (serviceClass == NULL)
        {
             //error handling
             NSLog(@"serviceClass is null!, can't callback and notifiy MacTrayIconService about mouse events.");
             return;
        }
        mid = (*JNU_GetEnv())->GetMethodID(JNU_GetEnv(), serviceClass, "mouseEventCallback", "(Ljava/lang/String;FFF)V");
        if (mid == NULL)
        {
             //error handling
             NSLog(@"method ID for 'mouseEventCallback' is null!, can't callback and notifiy MacTrayIconService about mouse events.");
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
                NSPoint screenPoint = [self convertToGlobalPoint:localPoint];
                NSPoint viewOriginPoint = [self convertToGlobalPoint:[self frame].origin];
                (*JNU_GetEnv())->CallVoidMethod(JNU_GetEnv(), _javaPeer, mid, eventType, screenPoint.x, screenPoint.y, viewOriginPoint.x );
            }
        }
   }

}


- (void)mouseDown:(NSEvent *)theEvent
{
	//NSLog(@"NATIVE MOUSE DOWN");
	jstring str  = CreateJavaStringFromNSString(JNU_GetEnv(), @"mouseDown");

    //try to notify the Java peer about this event
	[self doCallBackToJVM:str pointInWindow:[theEvent locationInWindow] ];



    /*
	NSPoint loc = [theEvent locationInWindow];
	NSLog(@"Hey, you clicked the left mouse button at x=%f, y=%f", loc.x, loc.y);
	NSPoint local_point = [self convertPoint:loc fromView:nil];
	NSLog(@"    converted point is x=%f, y=%f", local_point.x, local_point.y);

	NSPoint screenPoint = [[self window] convertBaseToScreen:local_point];
	NSLog(@"    screen point is x=%f, y=%f", screenPoint.x, screenPoint.y);
	*/
}

- (void)mouseUp:(NSEvent *)theEvent
{
	//NSLog(@"NATIVE MOUSE UP");

	jstring str = CreateJavaStringFromNSString(JNU_GetEnv(), @"mouseUp");
	[self doCallBackToJVM:str pointInWindow:[theEvent locationInWindow] ];
}


- (void)rightMouseDown:(NSEvent *)theEvent
{
	//NSLog(@"NATIVE RIGHT MOUSE DOWN");
	jstring str = CreateJavaStringFromNSString(JNU_GetEnv(), @"rightMouseDown");
	[self doCallBackToJVM:str pointInWindow:[theEvent locationInWindow]  ];

	/* NSPoint loc = [theEvent locationInWindow];
	NSLog(@"Hey, you clicked the right mouse button at x=%f, y=%f", loc.x, loc.y);
	NSPoint local_point = [self convertPoint:loc fromView:nil];
	NSLog(@"    converted point is x=%f, y=%f", local_point.x, local_point.y);
	NSPoint screenPoint = [[self window] convertBaseToScreen:local_point];
	NSLog(@"    screen point is x=%f, y=%f", screenPoint.x, screenPoint.y); */
}

- (void)rightMouseUp:(NSEvent *)theEvent
{
	//NSLog(@"NATIVE RIGHT MOUSE UP");

	jstring str = CreateJavaStringFromNSString(JNU_GetEnv(), @"rightMouseUp");
	[self doCallBackToJVM:str pointInWindow:[theEvent locationInWindow]  ];
}

- (void)drawRect:(NSRect)aRect
{
	[super drawRect:aRect];
	
	NSFont *font = [NSFont menuBarFontOfSize:0];
	NSDictionary *dict = [NSDictionary dictionaryWithObject:font forKey:NSFontAttributeName];
	
	NSGraphicsContext *gc = [NSGraphicsContext currentContext];
	[font setInContext:gc];
	
	if (_statusItem != nil)
	{
		if (isArmed == YES)
	    {
			[_statusItem drawStatusBarBackgroundInRect:[self frame] withHighlight:YES];
	        dict = [NSDictionary dictionaryWithObjectsAndKeys:font, NSFontAttributeName, [NSColor whiteColor],NSForegroundColorAttributeName,nil  ];
		}

	}
	
	
	//[font set];
	float x = 3.0;
	if (image != nil)
	{
		x += 22;
	}

	[labelText drawAtPoint:NSMakePoint(x,3) withAttributes:dict ];
}

- (void)setIsArmed:(BOOL)armedState
{
    isArmed = armedState;
    [self updateDisplay];
}

- (BOOL)isArmed
{
   return isArmed;
}


- (void)setJavaPeer:(jobject) peer
{
    _javaPeer = peer;
}

- (jobject)javaPeer
{
    return _javaPeer;
}

- (void)dealloc
{
	[self setLabelText:nil];
	[self setImage:nil];
	[imageView release];
	imageView = nil;
	_javaPeer = nil; //only parent object retains this
	[super dealloc];
}




@end
