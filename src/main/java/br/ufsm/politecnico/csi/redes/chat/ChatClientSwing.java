package br.ufsm.politecnico.csi.redes.chat;

import br.ufsm.politecnico.csi.redes.model.Mensagem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * User: Rafael
 * Date: 13/10/14
 * Time: 10:28
 *
 */
public class ChatClientSwing extends JFrame {

    private DatagramSocket socketSonda = new DatagramSocket(8084);
    private Socket socketTcp;
    private Usuario meuUsuario;
    private final String endBroadcast = "255.255.255.255";
    private JList listaChat;
    private DefaultListModel dfListModel;
    private JTabbedPane tabbedPane = new JTabbedPane();
    private Set<Usuario> chatsAbertos = new HashSet<>();

    public class RecebeSonda implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            ObjectMapper om = new ObjectMapper();
            while (true) {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                try {
                    socketSonda.receive(packet);
                    Mensagem sonda = om.readValue(buf, 0, packet.getLength(), Mensagem.class);

                    if (!sonda.getUsuario().equals(meuUsuario.nome)) {
                        atualizarHoraRecebimento(sonda.getUsuario());

//                        System.out.println("[SONDA RECEBIDA] " + sonda);
                        int idx = dfListModel.indexOf(new Usuario(sonda.getUsuario(),
                                StatusUsuario.valueOf(sonda.getStatus()), packet.getAddress()));
                        if (idx == -1) {
                            dfListModel.addElement(new Usuario(
                                    sonda.getUsuario(),
                                    StatusUsuario.valueOf(sonda.getStatus()),
                                    packet.getAddress(), System.currentTimeMillis())
                            );

                        } else {
                            Usuario usuario = (Usuario) dfListModel.getElementAt(idx);
                            usuario.setStatus(StatusUsuario.valueOf(sonda.getStatus()));
                            dfListModel.remove(idx);
                            dfListModel.add(idx, usuario);
                        }
                        verificarInatividade();
                    }


                } catch (IOException e) {
                    System.out.println("Erro ao receber pacote: " + e.getMessage());
                }
            }
        }

        private void atualizarHoraRecebimento(String nomeUsuario) {
            synchronized (dfListModel) {
                // Procure o usuário na lista dfListModel e atualize a hora de recebimento
                for (int i = 0; i < dfListModel.size(); i++) {
                    Usuario usuario = (Usuario) dfListModel.getElementAt(i);
                    if (usuario.getNome().equals(nomeUsuario)) {
                        usuario.setLastSonda(System.currentTimeMillis());
                        dfListModel.setElementAt(usuario, i);
                        break;
                    }
                }
            }
        }
        private void verificarInatividade() {
            long currentTime = System.currentTimeMillis();
            long inatividadeMaxima = 10000; // 30 segundos em milissegundos

            synchronized (dfListModel) {
                for (int i = 0; i < dfListModel.size(); i++) {
                    Usuario usuario = (Usuario) dfListModel.getElementAt(i);
                    long lastSondaReceivedTime = usuario.getLastSonda();
                    // Se o tempo decorrido for superior à inatividade máxima, remova o usuário
                    if (currentTime - lastSondaReceivedTime > inatividadeMaxima) {
                        //System.out.printf("CAIU");
                        dfListModel.removeElementAt(i); // Remove o usuário da lista
                    }
                }
            }
        }
    }


    public class EnviaSonda implements Runnable {

        @SneakyThrows
        @Override
        public void run() {
            synchronized (this) {
                if (meuUsuario == null) {
                    this.wait();
                }
            }
            while (true) {
                Mensagem mensagem = new Mensagem(
                        "SONDA_UDP",
                        meuUsuario.nome,
                        ChatClientSwing.this.meuUsuario.status.toString());
                ObjectMapper om = new ObjectMapper();
                byte[] msgJson = om.writeValueAsBytes(mensagem);
                //enviam sonda para lista de IPs
                for(int n = 0;  n < 255; n++) {
                    DatagramPacket packet = new DatagramPacket(
                            msgJson,
                            msgJson.length,
                            InetAddress.getByName(endBroadcast), 8084

                    );
                    socketSonda.send(packet);
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) { }
            }
        }
    }

    public ChatClientSwing() throws UnknownHostException, SocketException {
        setLayout(new GridBagLayout());
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Status");

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.DISPONIVEL.name());
        rbMenuItem.setSelected(true);
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.DISPONIVEL);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.NAO_PERTURBE.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.NAO_PERTURBE);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.VOLTO_LOGO.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.VOLTO_LOGO);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        menuBar.add(menu);
        this.setJMenuBar(menuBar);

        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popupMenu =  new JPopupMenu();
                    final int tab = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());
                    JMenuItem item = new JMenuItem("Fechar");
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PainelChatPVT painel = (PainelChatPVT) tabbedPane.getTabComponentAt(tab);
                            tabbedPane.remove(tab);
                            chatsAbertos.remove(painel.getUsuario());
                        }
                    });
                    popupMenu.add(item);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        add(new JScrollPane(criaLista()), new GridBagConstraints(0, 0, 1, 1, 0.1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        add(tabbedPane, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        setSize(800, 600);
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screenSize.width - this.getWidth()) / 2;
        final int y = (screenSize.height - this.getHeight()) / 2;
        this.setLocation(x, y);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Chat P2P - Redes de Computadores");
        String nomeUsuario = JOptionPane.showInputDialog(this, "Digite seu nome de usuário: ");

        synchronized (this) {
            this.meuUsuario = new Usuario(nomeUsuario, StatusUsuario.DISPONIVEL, InetAddress.getLocalHost());
            this.notify();
        }
        setVisible(true);
        new Thread(new RecebeSonda()).start();
        new Thread(new EnviaSonda()).start();
    }

    private JComponent criaLista() {
        dfListModel = new DefaultListModel();
        listaChat = new JList(dfListModel);
        listaChat.addMouseListener(new MouseAdapter() {
            @SneakyThrows
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                if (evt.getClickCount() == 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    Usuario user = (Usuario) list.getModel().getElementAt(index);
                    socketTcp = new Socket(user.getEndereco(), 8085);
                    if (chatsAbertos.add(user)) {
                        tabbedPane.add(user.toString(), new PainelChatPVT(user, socketTcp));
                    }
                }
            }
        });
        return listaChat;
    }

    class PainelChatPVT extends JPanel {

        JTextArea areaChat;
        JTextField campoEntrada;
        Usuario usuario;
        Socket socket;

        PainelChatPVT(Usuario usuario, Socket socket) {
            setLayout(new GridBagLayout());
            areaChat = new JTextArea();
            this.usuario = usuario;
            areaChat.setEditable(false);
            campoEntrada = new JTextField();
            this.socket = socket;
            new Thread(new RecebeMensagens()).start();
            campoEntrada.addActionListener(new ActionListener() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                    String mensagem = e.getActionCommand();
                    if (!mensagem.isEmpty()) {
                        ((JTextField) e.getSource()).setText(""); // Limpa o campo de entrada
                        enviarMensagem(mensagem);
                    }
                }
            });
            add(new JScrollPane(areaChat), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            add(campoEntrada, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        }

        private void enviarMensagem(String mensagem) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String mensagemSerializada = objectMapper.writeValueAsString(new Mensagem(meuUsuario.getNome(), mensagem));
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(mensagemSerializada);
                areaChat.append("[ " + meuUsuario.getNome() + " ]: " + mensagem + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void fecharConexao() {
            try {
                socket.close(); // Fecha o socket
                // Remove a aba do tabbedPane
                Component tabComponent = ChatClientSwing.this.tabbedPane.getComponentAt(tabbedPane.indexOfComponent(PainelChatPVT.this));
                if (tabComponent != null) {
                    tabbedPane.remove(tabComponent);
                    chatsAbertos.remove(usuario);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private class RecebeMensagens implements Runnable {
            @Override
            public void run() {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    while (true) {
                        String mensagemSerializada = in.readUTF();
                        Mensagem mensagem = objectMapper.readValue(mensagemSerializada, Mensagem.class);
                        areaChat.append("[ " + usuario.getNome() + " ]: " + mensagem.getText() + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public Usuario getUsuario() {
            return usuario;
        }

        public void setUsuario(Usuario usuario) {
            this.usuario = usuario;
        }

    }
    public enum StatusUsuario {
        DISPONIVEL, NAO_PERTURBE, VOLTO_LOGO
    }

    @Data
    public class Usuario {

        private String nome;
        private StatusUsuario status;
        private InetAddress endereco;
        private Long lastSonda;

        public Usuario(String nome, StatusUsuario status, InetAddress endereco) {
            this.nome = nome;
            this.status = status;
            this.endereco = endereco;
        }

        public Usuario(String nome, StatusUsuario status, InetAddress endereco, Long lastSonda) {
            this.nome = nome;
            this.status = status;
            this.endereco = endereco;
            this.lastSonda = lastSonda;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Usuario usuario = (Usuario) o;
            return Objects.equals(nome, usuario.nome) && Objects.equals(endereco, usuario.endereco);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nome, endereco);
        }

        public String toString() {
            return this.getNome() + " (" + getStatus().toString() + ")";
        }
    }
    public static void main(String[] args) throws UnknownHostException, SocketException {
        new ChatClientSwing();
    }
}
