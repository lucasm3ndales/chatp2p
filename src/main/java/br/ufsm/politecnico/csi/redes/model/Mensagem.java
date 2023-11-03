package br.ufsm.politecnico.csi.redes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class Mensagem {
    private int id;
    private String text;
    private String remetente;
    private String status;

    public Mensagem(int id,String text, String remetente, String status) {
        this.text = text;
        this.remetente = remetente;
        this.id = id;
        this.status = status;
    }

}
