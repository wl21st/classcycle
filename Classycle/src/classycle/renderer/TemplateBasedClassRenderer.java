/*
 * Copyright (c) 2003, Franz-Josef Elmer, All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */
package classycle.renderer;

import java.text.MessageFormat;

import classycle.ClassAttributes;
import classycle.graph.AtomicVertex;

/**
 *  Class vertex renderer based on a template and using
 *  <tt>java.text.MessageFormat</tt>.
 *
 *  @author Franz-Josef Elmer
 */
public class TemplateBasedClassRenderer implements AtomicVertexRenderer
{
  private MessageFormat _format;

  public TemplateBasedClassRenderer(String template)
  {
    _format = new MessageFormat(template);
  }

  /**
   * Renderes the specified vertex. It is assumed that the vertex attributes
   * are of the type {@link classycle.ClassAtributes}.
   * @param vertex Vertex to be rendered.
   * @return the rendered vertex.
   */
  public String render(AtomicVertex vertex)
  {
    String[] values = new String[7];
    ClassAttributes attributes = (ClassAttributes) vertex.getAttributes();
    values[0] = attributes.getName();
    values[1] = attributes.getType();
    values[2] = Long.toString(attributes.getSize());
    values[3] = attributes.isInnerClass() ? "true" : "false";
    values[4] = Integer.toString(vertex.getNumberOfIncomingArcs());
    int usesInternal = 0;
    int usesExternal = 0;
    for (int i = 0, n = vertex.getNumberOfOutgoingArcs(); i < n; i++)
    {
      if (((AtomicVertex) vertex.getHeadVertex(i)).isGraphVertex())
      {
        usesInternal++;
      }
      else
      {
        usesExternal++;
      }
    }
    values[5] = Integer.toString(usesInternal);
    values[6] = Integer.toString(usesExternal);
    return _format.format(values);
  }
} //class