/*
 * The following is a slightly modified version of a file from a newer version of GumTree. 
 * The Yaml parser is only in a version incompatible with Java 8.
 * Since GumTree is licensed under GNU GPL3, we can use this modification here. 
 */

/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2023 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.travis.parser;

import static com.github.gumtreediff.tree.TypeSet.type;

import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.events.DocumentStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

@Register(id = "yaml-snakeyaml", accept = {"\\.yml$", "\\.yaml$"}, priority = Registry.Priority.MAXIMUM)
public class YamlTreeGenerator extends TreeGenerator {
    @Override
    public TreeContext generate(Reader r) throws IOException {
        TreeContext ctx = new TreeContext();
        try {
            //LoaderOptions opts = new LoaderOptions();
            Parser p = new ParserImpl(new StreamReader(r));
            ITree root =  ctx.createTree(type("YamlDocument"));
            Stack<ITree> stack = new Stack<>();
            stack.push(root);
            ctx.setRoot(root);
            while (p.peekEvent() != null) {
                Event e = p.getEvent();
                if (e.is(Event.ID.DocumentStart)) {
                    DocumentStartEvent de = (DocumentStartEvent) e;
                    root.setPos(de.getStartMark().getIndex());
                } else if (e.is(Event.ID.DocumentEnd)) {
                    stack.peek().setLength(e.getEndMark().getIndex() - stack.peek().getPos());
                    stack.pop();
                } else if (e.is(Event.ID.MappingStart)) {
                    MappingStartEvent me = (MappingStartEvent) e;
                    ITree mappingNode = ctx.createTree(type("YamlHash"));
                    mappingNode.setPos(me.getStartMark().getIndex());
                    mappingNode.setParentAndUpdateChildren(stack.peek());
                    stack.push(mappingNode);
                } else if (e.is(Event.ID.MappingEnd)) {
                    stack.peek().setLength(e.getEndMark().getIndex() - stack.peek().getPos());
                    stack.pop();
                    if (stack.peek().getType() == type("YamlTuple")) {
                        stack.peek().setLength(e.getEndMark().getIndex() - stack.peek().getPos());
                        stack.pop();
                    }
                } else if (e.is(Event.ID.SequenceStart)) {
                    SequenceStartEvent se = (SequenceStartEvent) e;
                    ITree sequenceNode = ctx.createTree(type("YamlSequence"));
                    sequenceNode.setPos(se.getStartMark().getIndex());
                    sequenceNode.setParentAndUpdateChildren(stack.peek());
                    stack.push(sequenceNode);
                } else if (e.is(Event.ID.SequenceEnd)) {
                    stack.peek().setLength(e.getEndMark().getIndex() - stack.peek().getPos());
                    stack.pop();
                    if (stack.peek().getType() == type("YamlTuple")) {
                        stack.peek().setLength(e.getEndMark().getIndex() - stack.peek().getPos());
                        stack.pop();
                    }
                } else if (e.is(Event.ID.Scalar)) {
                    ScalarEvent se = (ScalarEvent) e;
                    if (stack.peek().getType() == type("YamlHash")) {
                        ITree tupleNode = ctx.createTree(type("YamlTuple"));
                        tupleNode.setParentAndUpdateChildren(stack.peek());
                        tupleNode.setPos(se.getStartMark().getIndex());
                        stack.push(tupleNode);
                        ITree keyNode = ctx.createTree(type("YamlTupleKey"));
                        keyNode.setPos(se.getStartMark().getIndex());
                        keyNode.setLength(se.getEndMark().getIndex() - se.getStartMark().getIndex());
                        keyNode.setLabel(se.getValue());
                        keyNode.setParentAndUpdateChildren(stack.peek());
                    }
                    else {
                        ITree scalarNode = ctx.createTree(type("YamlValue"));
                        scalarNode.setPos(se.getStartMark().getIndex());
                        scalarNode.setLength(se.getEndMark().getIndex() - se.getStartMark().getIndex());
                        scalarNode.setLabel(se.getValue());
                        scalarNode.setParentAndUpdateChildren(stack.peek());
                        if (stack.peek().getType() == type("YamlTuple")) {
                            stack.peek().setLength(e.getEndMark().getIndex() - stack.peek().getPos());
                            stack.pop();
                        }
                    }
                }
            }
        }
        catch (YAMLException e) {
            throw new SyntaxException(this, r);
        }
        return ctx;
    }
}