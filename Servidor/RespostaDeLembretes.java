import java.util.*;

/** Resposta do Backend 1 ao CT12. */
public class RespostaDeLembretes extends Comunicado
{
    private ArrayList<Lembrete> lembretes;

    public RespostaDeLembretes (ArrayList<Lembrete> lembretes)
    {
        this.lembretes = lembretes==null ? new ArrayList<Lembrete> () : lembretes;
    }

    public ArrayList<Lembrete> getLembretes ()
    {
        return this.lembretes;
    }
}
