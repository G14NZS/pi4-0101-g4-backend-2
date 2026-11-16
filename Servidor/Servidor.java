import java.util.*;

public class Servidor
{
	public static String PORTA_PADRAO = "3000";
	
    public static void main (String[] args)
    {
        if (args.length>1)
        {
            System.err.println ("Uso esperado: java Servidor [PORTA]\n");
            return;
        }

        String porta=Servidor.PORTA_PADRAO;
        
        if (args.length==1)
            porta = args[0];

        ArrayList<Parceiro> usuarios =
        new ArrayList<Parceiro> ();

        AceitadoraDeConexao aceitadoraDeConexao=null;
        try
        {
            aceitadoraDeConexao =
            new AceitadoraDeConexao (porta, usuarios);
            aceitadoraDeConexao.start();
        }
        catch (Exception erro)
        {
            System.err.println ("Escolha uma porta apropriada e liberada para uso!\n");
            return;
        }

        Agendador agendador = Servidor.inicieAgendador ();

        for(;;)
        {
            System.out.println ("O servidor esta ativo! Para desativa-lo,");
            System.out.println ("use o comando \"desativar\"\n");
            System.out.print   ("> ");

            String comando=null;
            try
            {
                comando = Teclado.getUmString();
            }
            catch (Exception erro)
            {}

            if (comando.toLowerCase().equals("desativar"))
            {
                if (agendador!=null)
                    agendador.desative ();

                synchronized (usuarios)
                {
					ComunicadoDeDesligamento comunicadoDeDesligamento =
                    new ComunicadoDeDesligamento ();
                    
                    for (Parceiro usuario:usuarios)
                    {
                        try
                        {
                            usuario.receba (comunicadoDeDesligamento);
                            usuario.adeus  ();
                        }
                        catch (Exception erro)
                        {}
                    }
                }

                System.out.println ("O servidor foi desativado!\n");
                System.exit(0);
            }
            else
                System.err.println ("Comando invalido!\n");
        }
    }

    /**
     * H08.2: liga o agendador de lembretes (CT12, CT19, CT13) com a configuracao das variaveis de
     * ambiente BACKEND1_HOST, BACKEND1_PORTA, CHAVE_INTERNA, TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN
     * e TWILIO_FROM.
     */
    private static Agendador inicieAgendador ()
    {
        String host  = Servidor.variavel ("BACKEND1_HOST", "localhost");
        String porta = Servidor.variavel ("BACKEND1_PORTA", "9090");
        String chave = Servidor.variavel ("CHAVE_INTERNA", "");

        if (chave.length() < 16)
        {
            System.err.println ("CHAVE_INTERNA ausente ou com menos de 16 caracteres; agendador de lembretes desligado.\n");
            return null;
        }

        try
        {
            ConexaoComBackend1 backend1 =
            new ConexaoComBackend1 (host, Integer.parseInt (porta));

            TwilioSms twilio =
            new TwilioSms (Servidor.variavel ("TWILIO_ACCOUNT_SID", ""),
                           Servidor.variavel ("TWILIO_AUTH_TOKEN",  ""),
                           Servidor.variavel ("TWILIO_FROM",        ""));

            Agendador agendador =
            new Agendador (backend1, twilio, chave, Agendador.INTERVALO_PADRAO);
            agendador.start ();

            System.out.println ("Agendador de lembretes ativo (Backend 1 em " + host + ":" + porta + ", a cada 15 minutos)\n");
            return agendador;
        }
        catch (Exception erro)
        {
            System.err.println ("Configuracao do agendador de lembretes invalida: " + erro.getMessage() + "\n");
            return null;
        }
    }

    private static String variavel (String nome, String padrao)
    {
        String valor = System.getenv (nome);
        return valor==null || valor.isBlank() ? padrao : valor;
    }
}
