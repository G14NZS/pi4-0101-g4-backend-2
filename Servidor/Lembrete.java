import java.io.*;
import java.time.*;
import java.time.format.*;

/** Um lembrete devolvido pelo Backend 1 no CT12 (agendamento que comeca nas proximas horas). */
public class Lembrete implements Serializable
{
    private static final DateTimeFormatter DIA  = DateTimeFormatter.ofPattern ("dd/MM");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern ("HH:mm");

    private int           idAgendamento;
    private String        telefone;
    private String        nomeCliente;
    private LocalDateTime dataHora;
    private String        servico;
    private String        profissional;

    public Lembrete (int idAgendamento, String telefone, String nomeCliente,
                     LocalDateTime dataHora, String servico, String profissional)
    throws Exception
    {
        if (telefone==null || telefone.isBlank())
            throw new Exception ("Telefone ausente");

        if (dataHora==null)
            throw new Exception ("Data e hora ausentes");

        this.idAgendamento = idAgendamento;
        this.telefone      = telefone;
        this.nomeCliente   = nomeCliente;
        this.dataHora      = dataHora;
        this.servico       = servico;
        this.profissional  = profissional;
    }

    public int           getIdAgendamento () { return this.idAgendamento; }
    public String        getTelefone      () { return this.telefone;      }
    public String        getNomeCliente   () { return this.nomeCliente;   }
    public LocalDateTime getDataHora      () { return this.dataHora;      }
    public String        getServico       () { return this.servico;       }
    public String        getProfissional  () { return this.profissional;  }

    /** Texto do SMS (campo Body do CT19). */
    public String getTextoDoSms ()
    {
        return "Olá, " + this.nomeCliente + "! Lembrete: " +
               this.dataHora.format(DIA) + " às " + this.dataHora.format(HORA) + ", " +
               this.servico + " com " + this.profissional + ".";
    }

    public String toString ()
    {
        return "Lembrete do agendamento " + this.idAgendamento;
    }
}
