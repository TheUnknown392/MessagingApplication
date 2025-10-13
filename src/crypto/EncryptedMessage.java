/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package crypto;

/**
 *
 * @author theunknown
 */
    public class EncryptedMessage {

        protected byte[] cypherText;
        protected byte[] iv;

        public EncryptedMessage(byte[] cypherText, byte[] iv) {
            this.cypherText = cypherText;
            this.iv = iv;
        }

        public byte[] getCypherText() {
            return this.cypherText;
        }

        public byte[] getIV() {
            return this.iv;
        }
    }
