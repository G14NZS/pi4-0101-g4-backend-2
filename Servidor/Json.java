import java.util.*;

/**
 * JSON minimo, so o suficiente para o protocolo do Backend 1 (CT12 e CT13): objetos, listas, textos,
 * numeros, booleanos e null. A leitura devolve Map (objeto), List (lista), String, Double, Boolean ou null.
 */
public class Json
{
    private String texto;
    private int    posicao = 0;

    private Json (String texto)
    {
        this.texto = texto;
    }

    // ---------- escrita ----------

    /** Texto JSON entre aspas, com os escapes necessarios. */
    public static String texto (String valor)
    {
        if (valor==null)
            return "null";

        StringBuilder sb = new StringBuilder ("\"");
        for (char c : valor.toCharArray())
        {
            switch (c)
            {
                case '"' : sb.append ("\\\""); break;
                case '\\': sb.append ("\\\\"); break;
                case '\n': sb.append ("\\n");  break;
                case '\r': sb.append ("\\r");  break;
                case '\t': sb.append ("\\t");  break;
                default:
                    if (c<0x20)
                        sb.append (String.format ("\\u%04x", (int)c));
                    else
                        sb.append (c);
            }
        }
        return sb.append ('"').toString ();
    }

    // ---------- leitura ----------

    public static Object leia (String texto) throws Exception
    {
        if (texto==null)
            throw new Exception ("JSON ausente");

        Json json = new Json (texto);
        Object valor = json.valor ();
        json.pulaEspacos ();
        if (json.posicao != texto.length())
            throw new Exception ("JSON com texto sobrando");
        return valor;
    }

    private Object valor () throws Exception
    {
        this.pulaEspacos ();
        if (this.posicao >= this.texto.length())
            throw new Exception ("JSON incompleto");

        char c = this.texto.charAt (this.posicao);
        if (c=='{') return this.objeto ();
        if (c=='[') return this.lista ();
        if (c=='"') return this.string ();
        if (this.confere ("true"))  return Boolean.TRUE;
        if (this.confere ("false")) return Boolean.FALSE;
        if (this.confere ("null"))  return null;
        return this.numero ();
    }

    private Map<String,Object> objeto () throws Exception
    {
        Map<String,Object> mapa = new LinkedHashMap<String,Object> ();
        this.posicao++; // {
        this.pulaEspacos ();
        if (this.proximo ('}'))
            return mapa;

        for (;;)
        {
            this.pulaEspacos ();
            String chave = this.string ();
            this.pulaEspacos ();
            this.exija (':');
            mapa.put (chave, this.valor ());
            this.pulaEspacos ();
            if (this.proximo ('}'))
                return mapa;
            this.exija (',');
        }
    }

    private List<Object> lista () throws Exception
    {
        List<Object> lista = new ArrayList<Object> ();
        this.posicao++; // [
        this.pulaEspacos ();
        if (this.proximo (']'))
            return lista;

        for (;;)
        {
            lista.add (this.valor ());
            this.pulaEspacos ();
            if (this.proximo (']'))
                return lista;
            this.exija (',');
        }
    }

    private String string () throws Exception
    {
        this.exija ('"');
        StringBuilder sb = new StringBuilder ();
        for (;;)
        {
            if (this.posicao >= this.texto.length())
                throw new Exception ("Texto JSON sem aspas finais");

            char c = this.texto.charAt (this.posicao++);
            if (c=='"')
                return sb.toString ();

            if (c!='\\')
            {
                sb.append (c);
                continue;
            }

            if (this.posicao >= this.texto.length())
                throw new Exception ("Escape JSON incompleto");

            char e = this.texto.charAt (this.posicao++);
            switch (e)
            {
                case 'n': sb.append ('\n'); break;
                case 'r': sb.append ('\r'); break;
                case 't': sb.append ('\t'); break;
                case 'b': sb.append ('\b'); break;
                case 'f': sb.append ('\f'); break;
                case 'u':
                    if (this.posicao+4 > this.texto.length())
                        throw new Exception ("Escape unicode incompleto");
                    sb.append ((char)Integer.parseInt (this.texto.substring (this.posicao, this.posicao+4), 16));
                    this.posicao += 4;
                    break;
                default: sb.append (e); // " \ /
            }
        }
    }

    private Double numero () throws Exception
    {
        int inicio = this.posicao;
        while (this.posicao < this.texto.length() &&
               "+-0123456789.eE".indexOf (this.texto.charAt (this.posicao)) != -1)
            this.posicao++;

        if (inicio==this.posicao)
            throw new Exception ("JSON invalido na posicao " + inicio);

        return Double.valueOf (this.texto.substring (inicio, this.posicao));
    }

    private boolean confere (String palavra)
    {
        if (!this.texto.startsWith (palavra, this.posicao))
            return false;
        this.posicao += palavra.length ();
        return true;
    }

    private boolean proximo (char c)
    {
        if (this.posicao < this.texto.length() && this.texto.charAt (this.posicao)==c)
        {
            this.posicao++;
            return true;
        }
        return false;
    }

    private void exija (char c) throws Exception
    {
        if (!this.proximo (c))
            throw new Exception ("JSON invalido: esperava '" + c + "' na posicao " + this.posicao);
    }

    private void pulaEspacos ()
    {
        while (this.posicao < this.texto.length() && Character.isWhitespace (this.texto.charAt (this.posicao)))
            this.posicao++;
    }
}
