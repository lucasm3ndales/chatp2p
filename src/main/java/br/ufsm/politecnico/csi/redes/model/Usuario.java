package br.ufsm.politecnico.csi.redes.model;

import br.ufsm.politecnico.csi.redes.chat.ChatClientSwing;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetAddress;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {
    private String nome;
    private StatusUsuario status;
    private InetAddress endereco;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(nome, usuario.nome) && Objects.equals(endereco, usuario.endereco);
    }
}
