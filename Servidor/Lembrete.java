import java.time.*;
import java.time.format.*;
import java.util.*;

/** Um lembrete pendente devolvido pelo Backend 1 no CT12 (objeto LembretePendente). */
public class Lembrete
{
    private static final DateTimeFormatter DIA  = DateTimeFormatter.ofPattern ("dd/MM");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern ("HH:mm");

    private int           idAgendamento;
    private String        telefone;
    private String        nomeCliente;
    private LocalDateTime inicio;
    private String        servico;
    private String        profissional;

    public Lembrete (int idAgendamento, String telefone, String nomeCliente,
                     LocalDateTime inicio, String servico, String profissional)
    throws Exception
    {
        if (telefone==null || telefone.isBlank())
            throw new Exception ("Telefone ausente");

        if (inicio==null)
            throw new Exception ("Data e hora ausentes");

        this.idAgendamento = idAgendamento;
        this.telefone      = telefone;
        this.nomeCliente   = nomeCliente;
        this.inicio        = inicio;
        this.servico       = servico;
        this.profissional  = profissional;
    }

    /** Monta o lembrete a partir do objeto LembretePendente do CT12. */
    public static Lembrete doJson (Map<?,?> o) throws Exception
    {
        Object id = o.get ("agendamentoId");
        if (!(id instanceof Number))
            throw new Exception ("agendamentoId ausente");

        return new Lembrete (((Number)id).intValue (),
                             texto (o.get ("clienteTelefone")),
                             texto (o.get ("clienteNome")),
                             LocalDateTime.parse (texto (o.get ("inicio"))),
                             texto (o.get ("servicoNome")),
                             texto (o.get ("profissionalNome")));
    }

    private static String texto (Object valor)
    {
        return valor instanceof String ? (String)valor : null;
    }

    public int           getIdAgendamento () { return this.idAgendamento; }
    public String        getTelefone      () { return this.telefone;      }
    public String        getNomeCliente   () { return this.nomeCliente;   }
    public LocalDateTime getInicio        () { return this.inicio;        }
    public String        getServico       () { return this.servico;       }
    public String        getProfissional  () { return this.profissional;  }

    /** Texto do SMS (campo Body do CT19). */
    public String getTextoDoSms ()
    {
        return "Olá, " + this.nomeCliente + "! Lembrete: " +
               this.inicio.format(DIA) + " às " + this.inicio.format(HORA) + ", " +
               this.servico + " com " + this.profissional + ".";
    }

    public String toString ()
    {
        return "Lembrete do agendamento " + this.idAgendamento;
    }
}
