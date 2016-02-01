import java.util.Comparator;


public class PacketComparator implements Comparator<MPacket>
{
    @Override
    public int compare(MPacket x, MPacket y)
    {
        // Assume neither string is null. Real code should
        // probably be more robust
        // You could also just return x.length() - y.length(),
        // which would be more efficient.
        if (x.sequenceNumber < y.sequenceNumber)
        {
            return -1;
        }
        if (x.sequenceNumber > y.sequenceNumber)
        {
            return 1;
        }
        return 0;
    }
}
