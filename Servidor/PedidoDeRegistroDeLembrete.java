/** CT13: Backend 2 registra no Backend 1 o resultado do envio de um lembrete. */
public class PedidoDeRegistroDeLembrete extends Comunicado
{
    public static final String ENVIADO = "enviado";
    public static final String FALHOU  = "falhou";

    private String chaveInterna;
    private int    idAgendamento;
    private String resultado;
    private String sid;    // preenchido quando enviado
    private String motivo; // preenchido quando falhou

    public PedidoDeRegistroDeLembrete (String chaveInterna, int idAgendamento,
                                       String resultado, String sid, String motivo)
    {
        this.chaveInterna  = chaveInterna;
        this.idAgendamento = idAgendamento;
        this.resultado     = resultado;
        this.sid           = sid;
        this.motivo        = motivo;
    }

    public String getChaveInterna  () { return this.chaveInterna;  }
    public int    getIdAgendamento () { return this.idAgendamento; }
    public String getResultado     () { return this.resultado;     }
    public String getSid           () { return this.sid;           }
    public String getMotivo        () { return this.motivo;        }

    public String toString ()
    {
        return "agendamento " + this.idAgendamento + ": " + this.resultado +
               (this.sid   !=null ? " (sid " + this.sid + ")" : "") +
               (this.motivo!=null ? " (" + this.motivo + ")"  : "");
    }
}
