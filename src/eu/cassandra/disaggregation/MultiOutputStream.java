/*
Copyright 2011-2013 The Cassandra Consortium (cassandra-fp7.eu)


Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package eu.cassandra.disaggregation;

import java.io.IOException;
import java.io.OutputStream;

public class MultiOutputStream extends OutputStream
{
  OutputStream[] outputStreams;

  public MultiOutputStream (OutputStream... outputStreams)
  {
    this.outputStreams = outputStreams;
  }

  @Override
  public void write (int b) throws IOException
  {
    for (OutputStream out: outputStreams)
      out.write(b);
  }

  @Override
  public void write (byte[] b) throws IOException
  {
    for (OutputStream out: outputStreams)
      out.write(b);
  }

  @Override
  public void write (byte[] b, int off, int len) throws IOException
  {
    for (OutputStream out: outputStreams)
      out.write(b, off, len);
  }

  @Override
  public void flush () throws IOException
  {
    for (OutputStream out: outputStreams)
      out.flush();
  }

  @Override
  public void close () throws IOException
  {
    for (OutputStream out: outputStreams)
      out.close();
  }
}
