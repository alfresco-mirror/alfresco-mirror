/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.content.filestore;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.MessageFormat;
import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.AbstractContentReader;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.RandomAccessContent;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides direct access to a local file.
 * <p>
 * This class does not provide remote access to the file.
 * 
 * @author Derek Hulley
 */
public class FileContentReader extends AbstractContentReader implements RandomAccessContent
{
    private static final Log logger = LogFactory.getLog(FileContentReader.class);
    
    private File file;
    
    /**
     * Checks the existing reader provided and replaces it with a reader onto some
     * fake content if required.  If the existing reader is invalid, an debug message
     * will be logged under this classname category.
     * <p>
     * It is a convenience method that clients can use to cheaply get a reader that
     * is valid, regardless of whether the initial reader is valid.
     * 
     * @param existingReader a potentially valid reader
     * @param msgTemplate the template message that will used to format the final <i>fake</i> content
     * @param args arguments to put into the <i>fake</i> content
     * @return Returns a the existing reader or a new reader onto some generated text content
     */
    public static ContentReader getSafeContentReader(ContentReader existingReader, String msgTemplate, Object ... args)
    {
        ContentReader reader = existingReader;
        if (existingReader == null || !existingReader.exists())
        {
            // the content was never written to the node or the underlying content is missing
            String fakeContent = MessageFormat.format(msgTemplate, args);
            
            // log it
            if (logger.isDebugEnabled())
            {
                logger.debug(fakeContent);
            }
            
            // fake the content
            File tempFile = TempFileProvider.createTempFile("getSafeContentReader_", ".txt");
            ContentWriter writer = new FileContentWriter(tempFile);
            writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
            writer.setEncoding("UTF-8");
            writer.putContent(fakeContent);
            // grab the reader from the temp writer
            reader = writer.getReader();
        }
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Created safe content reader: \n" +
                    "   existing reader: " + existingReader + "\n" +
                    "   safe reader: " + reader);
        }
        return reader;
    }
    
    /**
     * Constructor that builds a URL based on the absolute path of the file.
     * 
     * @param file the file for reading.  This will most likely be directly
     *      related to the content URL.
     */
    public FileContentReader(File file)
    {
        this(file, FileContentStore.STORE_PROTOCOL + file.getAbsolutePath());
    }
    
    /**
     * Constructor that explicitely sets the URL that the reader represents.
     * 
     * @param file the file for reading.  This will most likely be directly
     *      related to the content URL.
     * @param url the relative url that the reader represents
     */
    public FileContentReader(File file, String url)
    {
        super(url);
        
        this.file = file;
    }
    
    /**
     * @return Returns the file that this reader accesses
     */
    public File getFile()
    {
        return file;
    }

    public boolean exists()
    {
        return file.exists();
    }

    /**
     * @see File#length()
     */
    public long getSize()
    {
        if (!exists())
        {
            return 0L;
        }
        else
        {
            return file.length();
        }
    }
    
    /**
     * @see File#lastModified()
     */
    public long getLastModified()
    {
        if (!exists())
        {
            return 0L;
        }
        else
        {
            return file.lastModified();
        }
    }

    /**
     * The URL of the write is known from the start and this method contract states
     * that no consideration needs to be taken w.r.t. the stream state.
     */
    @Override
    protected ContentReader createReader() throws ContentIOException
    {
        return new FileContentReader(this.file, getContentUrl());
    }
    
    @Override
    protected ReadableByteChannel getDirectReadableChannel() throws ContentIOException
    {
        try
        {
            // the file must exist
            if (!file.exists())
            {
                throw new IOException("File does not exist");
            }
            // create the channel
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");  // won't create it
            FileChannel channel = randomAccessFile.getChannel();
            // done
            if (logger.isDebugEnabled())
            {
                logger.debug("Opened channel to file: " + file);
            }
            return channel;
        }
        catch (Throwable e)
        {
            throw new ContentIOException("Failed to open file channel: " + this, e);
        }
    }

    /**
     * @param directChannel a file channel
     */
    @Override
    protected ReadableByteChannel getCallbackReadableChannel(
            ReadableByteChannel directChannel,
            List<ContentStreamListener> listeners) throws ContentIOException
    {
        if (!(directChannel instanceof FileChannel))
        {
            throw new AlfrescoRuntimeException("Expected read channel to be a file channel");
        }
        FileChannel fileChannel = (FileChannel) directChannel;
        // wrap it
        FileChannel callbackChannel = new CallbackFileChannel(fileChannel, listeners);
        // done
        return callbackChannel;
    }

    /**
     * @return Returns false as this is a reader
     */
    public boolean canWrite()
    {
        return false;   // we only allow reading
    }

    public FileChannel getChannel() throws ContentIOException
    {
        // go through the super classes to ensure that all concurrency conditions
        // and listeners are satisfied
        return (FileChannel) super.getReadableChannel();
    }
}
