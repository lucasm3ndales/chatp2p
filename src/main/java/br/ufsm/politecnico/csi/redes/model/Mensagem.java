package br.ufsm.politecnico.csi.redes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class Mensagem {
    private String tipoMensagem;
    private String usuario;
    private String status;
    private String text;

    public  Mensagem(String usuario, String text) {
        this.usuario = usuario;
        this.text = text;
    }

    public  Mensagem(String tipoMensagem, String usuario, String status) {
        this.tipoMensagem = tipoMensagem;
        this.usuario = usuario;
        this.status = status;
    }


}
