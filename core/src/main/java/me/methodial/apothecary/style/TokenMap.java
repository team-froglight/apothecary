package me.methodial.apothecary.style;

import me.methodial.apothecary.theme.Ref;
import me.methodial.apothecary.theme.Token;
import me.methodial.apothecary.theme.TokenScope;

import java.util.HashMap;
import java.util.Map;

/**
 * Token values visible to one element: its own scoped values layered over its parent's.
 * Values may themselves be {@link Ref}s (computed from other tokens, resolved lazily here).
 */
public final class TokenMap implements TokenScope {
    public static final TokenMap EMPTY = new TokenMap(null, Map.of());

    private final TokenMap parent;
    private final Map<Token<?>, Object> own;
    private final Map<Token<?>, Object> cache = new HashMap<>();
    private int depth;

    public TokenMap(TokenMap parent, Map<Token<?>, Object> own) {
        this.parent = parent;
        this.own = own;
    }

    /** A child scope; returns {@code this} when there's nothing to add. */
    public TokenMap with(Map<Token<?>, Object> values) {
        if (values.isEmpty()) return this;
        return new TokenMap(this, new HashMap<>(values));
    }

    public boolean isEmpty() { return own.isEmpty() && (parent == null || parent.isEmpty()); }

    private Object raw(Token<?> t) {
        for (TokenMap m = this; m != null; m = m.parent) {
            Object v = m.own.get(t);
            if (v != null) return v;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Token<T> token) {
        Object c = cache.get(token);
        if (c != null) return (T) c;
        if (depth > 32) throw new IllegalStateException("Token cycle at " + token.name());
        depth++;
        try {
            Object v = raw(token);
            T result;
            if (v instanceof Ref<?> r) result = (T) r.resolve(this);
            else if (v != null) result = (T) v;
            else result = token.defaultValue(this);
            if (result != null) cache.put(token, result);
            return result;
        } finally {
            depth--;
        }
    }

    /** Same visible values (used to skip restyles when nothing changed). */
    public boolean sameAs(TokenMap o) { return this == o; }
}
