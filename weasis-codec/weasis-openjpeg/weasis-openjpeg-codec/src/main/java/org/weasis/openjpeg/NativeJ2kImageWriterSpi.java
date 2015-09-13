/*******************************************************************************
 * Copyright (c) 2015 Weasis Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 ******************************************************************************/
package org.weasis.openjpeg;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageOutputStream;

import com.sun.media.imageioimpl.common.ImageUtil;

/**
 */
public class NativeJ2kImageWriterSpi extends ImageWriterSpi {

    private boolean registered = false;

    public NativeJ2kImageWriterSpi() {
        super("Weasis Team", "1.0", NativeJ2kImageReaderSpi.NAMES, NativeJ2kImageReaderSpi.SUFFIXES,
            NativeJ2kImageReaderSpi.MIMES, NativeJ2kImageWriter.class.getName(),
            new Class[] { ImageOutputStream.class }, new String[] { NativeJ2kImageReaderSpi.class.getName() }, false,
            null, null, null, null, false, null, null, null, null);
    }

    @Override
    public void onRegistration(ServiceRegistry registry, Class category) {
        if (registered) {
            return;
        }
        registered = true;

        List list = ImageUtil.getJDKImageReaderWriterSPI(registry, "JPEG 2000", false);

        for (int i = 0; i < list.size(); i++) {
            // Set this codec to higher priority
            registry.setOrdering(category, this, list.get(i));
        }
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        ColorModel colorModel = type.getColorModel();

        if (colorModel instanceof IndexColorModel) {
            // No need to check further: writer converts to 8-8-8 RGB.
            return true;
        }

        SampleModel sampleModel = type.getSampleModel();

        // Ensure all channels have the same bit depth
        int bitDepth;
        if (colorModel != null) {
            int[] componentSize = colorModel.getComponentSize();
            bitDepth = componentSize[0];
            for (int i = 1; i < componentSize.length; i++) {
                if (componentSize[i] != bitDepth) {
                    return false;
                }
            }
        } else {
            int[] sampleSize = sampleModel.getSampleSize();
            bitDepth = sampleSize[0];
            for (int i = 1; i < sampleSize.length; i++) {
                if (sampleSize[i] != bitDepth) {
                    return false;
                }
            }
        }

        // Ensure bitDepth is no more than 16
        if (bitDepth > 16) {
            return false;
        }

        // Check number of bands.
        int numBands = sampleModel.getNumBands();
        if (numBands < 1 || numBands > 4) {
            return false;
        }

        return true;
    }

    @Override
    public String getDescription(Locale locale) {
        return "Natively-accelerated JPEG2000 Image Writer (OpenJPEG based)";
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) throws IOException {
        return new NativeJ2kImageWriter(this);
    }
}
