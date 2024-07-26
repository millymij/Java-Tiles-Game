package game2D;
import java.io.*;

public class ReverseFilter extends FilterInputStream {

    ReverseFilter(InputStream in) { super(in); }

    /**
     *  Get a value from the array 'buffer' at the given 'position' and convert it into short format
     * @param buffer - the array to get the value from
     * @param position - the position in the array to get the value from
     * @return
     */
    public short getSample(byte[] buffer, int position)
    {
        return (short) (((buffer[position+1] & 0xff) << 8) |
                (buffer[position] & 0xff));
    }

    /**
     * Set a short value 'sample' in the array 'buffer' at the given position in little-endian format
     * @param buffer - the array to set the value in
     * @param position - the position in the array to set the value in
     * @param sample - the value to set
     */
    public void setSample(byte[] buffer, int position, short sample)
    {
        buffer[position] = (byte)(sample & 0xFF);
        buffer[position+1] = (byte)((sample >> 8) & 0xFF);
    }

    /**
     * Read a sample of data from the input stream and reverse it
     * @param sample     the buffer into which the data is read
     * @param offset   the start offset in the destination array
     * @param length   the maximum number of bytes read
     * @return
     * @throws IOException
     */
    public int read(byte [] sample, int offset, int length) throws IOException
    {
        int bytesRead = super.read(sample,offset,length);
        byte[] buffer = new byte [bytesRead];
        short amp;
        int	p;

        for (p =0; p < bytesRead; p++) buffer[p] = sample[p];

        for (p=0; p<bytesRead; p = p + 2)
        {
            short startSample = getSample(sample, p);
            int oppositeP = offset + bytesRead - (p - offset) - 2;
            short endSample = getSample(sample, oppositeP);
            setSample(sample, p, endSample);

            setSample(sample, oppositeP, startSample);

            if (p == offset + bytesRead / 2 - 2) {
                break;
            }
        }
        return length;
    }
}
