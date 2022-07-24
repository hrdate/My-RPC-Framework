package common.compress;

/**
 * 压缩字节码
 */
public interface Compress {

    byte[] compress(byte[] bytes);


    byte[] decompress(byte[] bytes);
}
