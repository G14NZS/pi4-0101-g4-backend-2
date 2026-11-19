/** CT12: Backend 2 pede ao Backend 1 os lembretes das proximas horas ainda nao registrados. */
public class PedidoDeLembretes extends Comunicado
{
    private String chaveInterna;
    private int    horas;

    public PedidoDeLembretes (String chaveInterna, int horas)
    {
        this.chaveInterna = chaveInterna;
        this.horas        = horas;
    }

    public String getChaveInterna () { return this.chaveInterna; }
    public int    getHoras        () { return this.horas;        }
}
