package catdata.provers;

import catdata.Chc;
import catdata.Pair;
import catdata.ParseException;
import catdata.Util;
import catdata.cql.AqlJs;
import catdata.cql.AqlOptions;
import catdata.cql.AqlProver.ProverName;
import catdata.cql.Collage;
import catdata.cql.Collage.CCollage;
import catdata.cql.Constraints;
import catdata.cql.DP;
import catdata.cql.ED;
import catdata.cql.Eq;
import catdata.cql.Instance;
import catdata.cql.Term;
import catdata.cql.exp.Att;
import catdata.cql.exp.CombinatorParser;
import catdata.cql.exp.Fk;
import catdata.cql.exp.RawTerm;
import catdata.cql.exp.Sym;
import catdata.cql.fdm.InitialAlgebra;
import catdata.cql.fdm.SaturatedInstance;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EgglogProver<T, C, V> extends DPKB<T, C, V> {

  BufferedReader reader;
  BufferedWriter writer;
  BufferedReader err;
  Process proc;
  int runLevel;

  String it = "";
  String ot = "";
  int count = 0;

  public EgglogProver(String exePath, KBTheory<T, C, V> th, int r) {
    super(th);
    runLevel = r;
    String sb = toEgglog(th);
    File f = new File(exePath);
    if (!f.exists()) {
      throw new RuntimeException("File does not exist: " + exePath);
    }

    try {
      String str = exePath;
      proc = Runtime.getRuntime().exec(str);
      writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
      reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
      it = sb;
      writer.write(sb.toString() + "\n");
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("1Internal theorem prover anomaly: " + e.getLocalizedMessage());
    }
  }

  static Function<Chc<?, String>, String> pr =
      z -> z.toString().replace("inr ", "").replace("inl ", "");
  static Function<Map<String, Chc<String, String>>, String> qr =
      z -> {
        String ret = "";
        for (var w : z.entrySet()) {
          var d = " (univ" + pr.apply(w.getValue()) + " " + w.getKey() + ") ";
          ret += d;
        }
        return ret;
      };

  public static String toEgglog(Constraints eds, Instance I) {
    StringBuffer sb = new StringBuffer();

    var kb = I.collage().toKB();

    int ruleNo = 1;
    for (ED ed : eds.eds) {
      String lhs = "";
      String rhs = "";
      String pre = "R" + (ruleNo++);

      List<Chc<String, String>> l = new LinkedList<>();
      List<Term<String, String, Sym, Fk, Att, Void, Void>> r = new LinkedList<>();

      Map<String, Term<String, String, Sym, Fk, Att, Void, Void>> subst = new HashMap<>();
      for (var y : ed.As.entrySet()) {
        l.add(y.getValue());
        r.add(Term.Var(y.getKey()));
        lhs += "(univ" + pr.apply(y.getValue()) + " " + y.getKey() + " )";
      }
      for (var x : ed.Es.entrySet()) {
        var ss = new Pair<>(l, x.getValue());

        var tt =
            new Pair<>(l.stream().map(pr).collect(Collectors.toList()), pr.apply(x.getValue()));
        kb.syms.put(pre + x.getKey(), ss);
        subst.put(x.getKey(), Term.Sym(Sym.Sym(pre + x.getKey(), tt), r));
      }
      for (var eq : ed.Awh) {
        lhs += "(= " + eq.first.toEgglog() + " " + eq.second.toEgglog() + ")";
      }
      for (var eq : ed.Ewh) {
        rhs +=
            "(union "
                + eq.first.subst(subst).toEgglog()
                + " "
                + eq.second.subst(subst).toEgglog()
                + ")";
      }
      sb.append("\n(rule (" + lhs + ") " + "(" + rhs + "))"); //
    }

    kb.validate();
    var x = toEgglog(kb) + sb.toString();
    return x;
  }

  private static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> String readX(
      String s, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I) {
    var i = s.trim().indexOf(" ");
    return s.trim().substring(i + 2, s.trim().length() - 2);
  }

  public static String toEgglog(Constraints eds, Constraints c2) {

    StringBuffer sb = new StringBuffer();

    int ruleNo = 1;
    for (ED ed : eds.eds) {
      String lhs = "";
      String rhs = "";
      String pre = "R" + (ruleNo++);

      List<Chc<String, String>> l = new LinkedList<>();
      List<Term<String, String, Sym, Fk, Att, Void, Void>> r = new LinkedList<>();

      Map<String, Term<String, String, Sym, Fk, Att, Void, Void>> subst = new HashMap<>();
      for (var y : ed.As.entrySet()) {
        l.add(y.getValue());
        r.add(Term.Var(y.getKey()));
        lhs += "(univ" + pr.apply(y.getValue()) + " " + y.getKey() + " )";
      }
      for (var x : ed.Es.entrySet()) {
        var ss = new Pair<>(l, x.getValue());

        var tt =
            new Pair<>(l.stream().map(pr).collect(Collectors.toList()), pr.apply(x.getValue()));
        // add syms
        //				kb.syms.put(pre + x.getKey(), ss);
        subst.put(x.getKey(), Term.Sym(Sym.Sym(pre + x.getKey(), tt), r));
      }
      for (var eq : ed.Awh) {
        lhs += "(= " + eq.first.toEgglog() + " " + eq.second.toEgglog() + ")";
      }
      for (var eq : ed.Ewh) {
        rhs +=
            "(union "
                + eq.first.subst(subst).toEgglog()
                + " "
                + eq.second.subst(subst).toEgglog()
                + ")";
      }
      sb.append("\n(rule (" + lhs + ") " + "(" + rhs + "))"); //
    }

    var x = toEgglog(eds.schema.collage().toKB()) + sb.toString();

    return x;
  }

  public static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> boolean entails(
      Constraints eds1, Constraints eds2, String exePath) {

    String sb = toEgglog(eds1, eds2);

    File f = new File(exePath);
    if (!f.exists()) {
      throw new RuntimeException("File does not exist: " + exePath);
    }

    try {
      String str = exePath;
      var proc = Runtime.getRuntime().exec(str);

      var writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
      var reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      var err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

      writer.write(sb.toString() + "\n");

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("1Internal theorem prover anomaly: " + e.getLocalizedMessage());
    }
    return false;
  }

  public static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
      SaturatedInstance<
              Ty, En, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
          egglogChase(
              String exePath,
              Constraints eds,
              Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I,
              AqlOptions ops) {

    String sb = toEgglog(eds, I);

    File f = new File(exePath);
    if (!f.exists()) {
      throw new RuntimeException("File does not exist: " + exePath);
    }

    try {
      String str = exePath;
      var proc = Runtime.getRuntime().exec(str);

      var writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
      var reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      var err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

      writer.write(sb.toString() + "\n");
      writer.write("(run-schedule (saturate (run)))" + "\n");
      writer.flush();

      while (true) {
        String w = err.readLine();
        if (w.contains("Ruleset : search ")) {
          break;
        }
      }
      try {
        Thread.currentThread().sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      Collage<Ty, En, Sym, Fk, Att, String, String> col = new CCollage<>();
      col.addAll(I.schema().typeSide.collage());
      col.addAll(I.schema().collage());

      try {
        for (var en : I.schema().ens) {
          writer.write("(print-size " + "univ" + en + ")\n");
          writer.flush();
          String i = reader.readLine();

          int j = Integer.parseInt(i);

          writer.write("(print-function " + "univ" + en + " " + j + ")\n");
          writer.flush();

          reader.readLine(); // (
          for (int w = 0; w < j; w++) {
            String k = reader.readLine();
            var h = readX(k, I);
            col.gens().put(h, en);
          }
          reader.readLine(); // )
          reader.readLine(); //
        }
        for (var g : col.gens().entrySet()) {
          for (var att : I.schema().attsFrom(g.getValue())) {
            writer.write("(extract " + "(" + att + "(" + g.getKey() + ")))\n");
            writer.flush();
            String z = reader.readLine(); // (
            var j =
                RawTerm.infer1x(
                    Collections.emptyMap(),
                    toRawTerm(z, col.gens()),
                    null,
                    null,
                    (Collage) col,
                    "",
                    (AqlJs<String, catdata.cql.exp.Sym>) I.schema().typeSide.js);
            col.eqs().add(new Eq(null, Term.Att(att, Term.Gen(g.getKey())), (Term) j.second));
          }
        }
      } catch (ParseException ex) {
        throw new RuntimeException(ex);
      }

      DP<Ty, En, Sym, Fk, Att, String, String> dp =
          new DP<>() {

            @Override
            public String toStringProver() {
              return "Egglog-chase";
            }

            @Override
            public boolean eq(
                Map<String, Chc<Ty, En>> ctx,
                Term<Ty, En, Sym, Fk, Att, String, String> lhs,
                Term<Ty, En, Sym, Fk, Att, String, String> rhs) {
              if (ctx != null && !ctx.isEmpty()) {
                throw new RuntimeException("Cannot solve full word problem");
              }
              try {
                writer.write("(check (= " + lhs.toEgglog() + " " + rhs.toEgglog() + "))\n");
                writer.flush();

                while (true) {
                  String w = err.readLine();
                  if (w.contains("Check failed")) {
                    return false;
                  } else if (w.contains("Checked fact")) {
                    return true;
                  }
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          };

      var ret =
          new InitialAlgebra<Ty, En, Sym, Fk, Att, String, String>(
              ops,
              I.schema(),
              col,
              z -> z.toString(),
              (z, zz) -> zz.toString(),
              dp,
              ProverName.egglog);
      return new SaturatedInstance<>(ret, ret, false, false, true, new HashMap<>());

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("1Internal theorem prover anomaly: " + e.getLocalizedMessage());
    }
  }

  private static <En> RawTerm toRawTerm(String z, Map<String, En> gens) throws ParseException {
    return toRawHelper(new CombinatorParser().parseEgglogRawTerm(z), gens);
  }

  private static <En> RawTerm toRawHelper(RawTerm t, Map<String, En> gens) {
    for (String x : gens.keySet()) {
      // System.out.println("compare " + t.toStringEgglog() + " and " + "(" + x +
      // ")");
      if (t.toStringEgglog().equals("(" + x + ")")) {
        return new RawTerm(x, new LinkedList<>());
      } else {
        // System.out.println("no");
      }
    }
    return new RawTerm(
        t.head, t.args.stream().map(z -> toRawHelper(z, gens)).collect(Collectors.toList()));
  }

  public static <T, C, V> String toEgglog(KBTheory<T, C, V> th) {
    StringBuffer sig = new StringBuffer();
    StringBuffer ground = new StringBuffer();
    StringBuffer eqs = new StringBuffer();
    StringBuffer univ = new StringBuffer();
    StringBuffer univR = new StringBuffer();

    Map<T, List<String>> outer = Util.newListsFor(th.tys);
    Map<T, List<String>> outer2 = Util.newListsFor(th.tys);

    Function<T, String> pr = z -> z.toString().replace("inr ", "").replace("inl ", "");

    sig.append("(datatype*");
    for (C c : th.syms.keySet()) {
      var ty = th.syms.get(c);
      String s =
          "("
              + c
              + " "
              + Util.sep(ty.first.stream().map(j -> pr.apply(j)).collect(Collectors.toList()), " ")
              + ")";
      outer.get(ty.second).add(s);
      if (ty.first.isEmpty()) {
        ground.append("(" + c + ")\n");
      }
    }

    for (T ty : th.tys) {
      sig.append("\n(" + pr.apply(ty) + "\n" + Util.sep(outer.get(ty), "\n") + ")");
      univ.append("\n(relation univ" + pr.apply(ty) + " (" + pr.apply(ty) + "))");
    }
    eqs.append("\n");
    for (C c : th.syms.keySet()) {
      var ty = th.syms.get(c);
      String ww = "";
      String cc = "";
      for (int i = 1; i <= ty.first.size(); i++) {
        cc += (" var" + i + " ");
        ww += ("(univ" + pr.apply(ty.first.get(i - 1)) + " " + "var" + i + ")");
      }
      String s =
          "(rule ((= var0 ("
              + c
              + " "
              + cc.trim()
              + "))) ((univ"
              + pr.apply(ty.second)
              + " var0)))";
      outer2.get(ty.second).add(s);
      String t = "(rule (" + ww + ")" + "((" + c + " " + cc.trim() + ")))";
      if (ty.first.size() > 0) {
        outer2.get(ty.second).add(t);
      }
    }
    sig.append("\n)");
    for (T ty : th.tys) {
      univR.append("\n" + Util.sep(outer2.get(ty), "\n"));
    }
    Function<Map<V, T>, String> qr =
        z -> {
          String ret = "";
          for (var w : z.entrySet()) {
            var d = "(univ" + pr.apply(w.getValue()) + " " + w.getKey() + ")";
            ret += d;
          }
          return ret;
        };
    for (var eq : th.eqs) {
      String tail =
          eq.first == null || eq.first.isEmpty() ? "" : " :when (" + qr.apply(eq.first) + ")";
      if (eq.first != null && eq.first.size() > 0) {
        eqs.append("\n(birewrite " + eq.second.toEgglog() + " " + eq.third.toEgglog() + tail + ")");
      } else {
        eqs.append("\n(union " + eq.second.toEgglog() + " " + eq.third.toEgglog() + ")");
      }
    }
    return sig.toString()
        + "\n"
        + ground.toString()
        + "\n"
        + univ
        + "\n"
        + univR
        + "\n"
        + eqs.toString()
        + "\n";
  }

  @Override
  public synchronized boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
    try {

      if (ctx != null && !ctx.isEmpty()) {
        writer.write("(push)" + "\n");
        writer.flush();
        String w = err.readLine();
        for (var e : ctx.entrySet()) {
          writer.write(
              "(constructor " + e.getKey() + " () " + pr.apply((Chc) e.getValue()) + ")\n");
          writer.flush();
          w = err.readLine();

          writer.write("(univ" + pr.apply((Chc) e.getValue()) + "( " + e.getKey() + "))\n");
          writer.flush();
          w = err.readLine();
          String s =
              "(rule ((= var0 ("
                  + e.getKey()
                  + " "
                  + "))) ((univ"
                  + (pr.apply((Chc) e.getValue()))
                  + " var0)))\n";
          writer.write(s);
          writer.flush();
          w = err.readLine();
        }
      }

      writer.write(
          "(run " + runLevel + " :until (= " + lhs.toEgglog() + " " + rhs.toEgglog() + "))" + "\n");
      writer.flush();

      writer.write("(check (= " + lhs.toEgglog() + " " + rhs.toEgglog() + "))\n");
      writer.flush();

      while (true) {
        String w = err.readLine();
        if (w.contains("Check failed")) {

          if (ctx != null && !ctx.isEmpty()) {
            writer.write("(pop)" + "\n");
            writer.flush();
          }

          return false;
        } else if (w.contains("Checked fact")) {

          if (ctx != null && !ctx.isEmpty()) {
            writer.write("(pop)" + "\n");
            writer.flush();
          }

          return true;
        }
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    var g = it + "\n\n" + ot;
    return "Egglog prover\n\n" + g.substring(0, Integer.min(g.length(), 4096 * 4096));
  }

  @Override
  public void add(C c, T t) {
    throw new RuntimeException("Not supported yet");
  }

  @Override
  public boolean supportsTrivialityCheck() {
    return false;
  }
}
