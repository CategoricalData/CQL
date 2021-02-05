package catdata.aql;

import java.util.Optional;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Language;
import org.graalvm.polyglot.Value;

public class ExternalCodeUtils implements AutoCloseable {

  public static final String LANG_JS = "js";

	private final Context context = Context.newBuilder()
      .allowHostAccess(HostAccess.ALL)
      .allowHostClassLookup(className -> true)
      .build();

  private Value eval(String lang, String source) {
    return context.eval(lang, source);
  }

  public <A> A eval(String lang, Class<A> clazz, String source) {
    return eval(lang, source).as(clazz);
  }

  public <A> A invoke(String lang, Class<A> clazz, String source, Object... args) {
    return eval(lang, source).execute(args).as(clazz);
  }

  public void bind(String lang, String var, Object value) {
    context.getBindings(lang).putMember(var, value);
  }

  public Optional<String> getLanguageName(String id) {
    return Optional.ofNullable(context.getEngine().getLanguages().get(id)).map(Language::getName);
  }

  public void close() {
    context.close();
  }
}
