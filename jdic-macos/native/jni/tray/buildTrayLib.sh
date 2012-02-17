#!/bin/sh

#thanks to Jerome Dochez for a blog entry about the universal flags for gcc. Sure beats having to have an Xcode project for this!
#Method 1 thanks to Doug Zwick
gcc -std=gnu99 -arch x86_64 -isysroot /Developer/SDKs/MacOSX10.6.sdk -I/System/Library/Frameworks/JavaVM.framework/Headers -I/System/Library/Frameworks/Cocoa.framework/Headers -dynamiclib -o ../libtray.jnilib *.m -framework JavaVM -framework Cocoa

#Method 2 thanks to Shawn Erickson and Pratik Solanki
#gcc -std=gnu99 -c -arch i386 -arch ppc -isysroot /Developer/SDKs/MacOSX10.4u.sdk -I/System/Library/Frameworks/JavaVM.framework/Headers -I/System/Library/Frameworks/Cocoa.framework/Headers *.m
#gcc -isysroot /Developer/SDKs/MacOSX10.4u.sdk/ -arch i386 -arch ppc -dynamiclib -o ../libtray.jnilib *.o -framework JavaVM -framework Cocoa

    
