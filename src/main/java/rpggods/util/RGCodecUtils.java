package rpggods.util;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.List;
import java.util.function.Function;

public final class RGCodecUtils {
    /**
     * @param codec an element codec
     * @param <T> the element type
     * @return a codec that allows either a single element or a list of elements
     */
    public static <T> Codec<List<T>> listOrElementCodec(final Codec<T> codec) {
        return Codec.either(codec, codec.listOf())
                .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));
    }
}
