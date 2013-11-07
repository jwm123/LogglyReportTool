/**
 *    Copyright 2013 jwm123
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.jwm123.loggly.reporter;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * com.jwm123.loggly.reporter.TripleDesCipher
 *
 * @author jmcentire
 */
public class TripleDesCipher {
  private static String TRIPLE_DES_TRANSFORMATION = "DESede/ECB/PKCS5Padding";
  private static String ALGORITHM = "DESede";
  private Cipher encrypter;
  private Cipher decrypter;
  private byte[] key;
  public String keyPath;
  public AppDirectory appDir;

  public TripleDesCipher(String keyPath, AppDirectory appDir) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
      InvalidKeyException, IOException {
    this.appDir = appDir;
    if(key == null) {
      this.keyPath = keyPath;
      getKey();
    }
    SecretKey keySpec = new SecretKeySpec(key, ALGORITHM);
    encrypter = Cipher.getInstance(TRIPLE_DES_TRANSFORMATION);
    encrypter.init(Cipher.ENCRYPT_MODE, keySpec);
    decrypter = Cipher.getInstance(TRIPLE_DES_TRANSFORMATION);
    decrypter.init(Cipher.DECRYPT_MODE, keySpec);
  }

  public String encode(String str) throws IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
    return new String(Base64.encode(encrypter.doFinal(str.getBytes("UTF8"))));
  }

  public String decode(String str) throws IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
    return new String(decrypter.doFinal(Base64.decode(str)), "UTF8");
  }

  private void getKey() throws NoSuchAlgorithmException, IOException {
    File keyFile = appDir.getFileDir(keyPath);
    if(keyFile.exists()) {
      key = Base64.decode(FileUtils.readFileToString(keyFile));
    } else {
      KeyGenerator generator = KeyGenerator.getInstance("DESede");
      SecretKey desKey = generator.generateKey();
      key = desKey.getEncoded();
      FileUtils.writeStringToFile(keyFile, new String(Base64.encode(key)));
    }

  }
}
