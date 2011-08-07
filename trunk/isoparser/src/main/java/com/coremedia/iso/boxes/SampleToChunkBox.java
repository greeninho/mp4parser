/*  
 * Copyright 2008 CoreMedia AG, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an AS IS BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

package com.coremedia.iso.boxes;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.IsoBufferWrapper;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoOutputStream;

import java.io.IOException;
import java.util.*;

/**
 * Samples within the media data are grouped into chunks. Chunks can be of different sizes, and the
 * samples within a chunk can have different sizes. This table can be used to find the chunk that
 * contains a sample, its position, and the associated sample description. Defined in ISO/IEC 14496-12.
 */
public class SampleToChunkBox extends AbstractFullBox {
    List<Entry> entries = Collections.emptyList();

    public static final String TYPE = "stsc";

    public SampleToChunkBox() {
        super(IsoFile.fourCCtoBytes(TYPE));
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public String getDisplayName() {
        return "Sample to Chunk Box";
    }

    protected long getContentSize() {
        return entries.size() * 12 + 4;
    }

    public void parse(IsoBufferWrapper in, long size, BoxParser boxParser, Box lastMovieFragmentBox) throws IOException {
        super.parse(in, size, boxParser, lastMovieFragmentBox);
        long entryCount = in.readUInt32();
        if (entryCount > Integer.MAX_VALUE) {
            throw new IOException("The parser cannot deal with more than Integer.MAX_VALUE entries!");
        }
        entries = new ArrayList<Entry>((int) entryCount);
        for (int i = 0; i < entryCount; i++) {
            entries.add(new Entry(in.readUInt32(), in.readUInt32(), in.readUInt32()));
        }
    }

    protected void getContent(IsoOutputStream isos) throws IOException {
        long l = isos.getStreamPosition();
        isos.writeUInt32(entries.size());
        for (Entry entry : entries) {
            isos.writeUInt32(entry.getFirstChunk());
            isos.writeUInt32(entry.getSamplesPerChunk());
            isos.writeUInt32(entry.getSampleDescriptionIndex());

        }
        assert getContentSize() == (isos.getStreamPosition() - l);
    }

    public String toString() {
        return "SampleToChunkBox[entryCount=" + entries.size() + "]";
    }

    /**
     * Decompresses the list of entries and returns the number of samples per chunk for
     * every single chunk.
     * @param chunkCount
     * @return number of samples per chunk
     */
    public long[] blowup(int chunkCount) {
        long[] numberOfSamples = new long[chunkCount];
        int j = 0;
        List<SampleToChunkBox.Entry> sampleToChunkEntries = new LinkedList<Entry>(entries);
        Collections.reverse(sampleToChunkEntries);
        Iterator<Entry> iterator = sampleToChunkEntries.iterator();
        SampleToChunkBox.Entry currentEntry = iterator.next();

        for (int i = numberOfSamples.length; i > 1; i--) {
            numberOfSamples[i - 1] = currentEntry.getSamplesPerChunk();
            if (i == currentEntry.getFirstChunk()) {
                currentEntry = iterator.next();
            }
        }
        numberOfSamples[0] = currentEntry.getSamplesPerChunk();
        return numberOfSamples;
    }

    public static class Entry {
        long firstChunk;
        long samplesPerChunk;
        long sampleDescriptionIndex;

        public Entry(long firstChunk, long samplesPerChunk, long sampleDescriptionIndex) {
            this.firstChunk = firstChunk;
            this.samplesPerChunk = samplesPerChunk;
            this.sampleDescriptionIndex = sampleDescriptionIndex;
        }

        public long getFirstChunk() {
            return firstChunk;
        }

        public void setFirstChunk(long firstChunk) {
            this.firstChunk = firstChunk;
        }

        public long getSamplesPerChunk() {
            return samplesPerChunk;
        }

        public void setSamplesPerChunk(long samplesPerChunk) {
            this.samplesPerChunk = samplesPerChunk;
        }

        public long getSampleDescriptionIndex() {
            return sampleDescriptionIndex;
        }

        public void setSampleDescriptionIndex(long sampleDescriptionIndex) {
            this.sampleDescriptionIndex = sampleDescriptionIndex;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "firstChunk=" + firstChunk +
                    ", samplesPerChunk=" + samplesPerChunk +
                    ", sampleDescriptionIndex=" + sampleDescriptionIndex +
                    '}';
        }
    }
}
