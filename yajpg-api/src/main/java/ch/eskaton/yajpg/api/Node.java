/*
w *  Copyright (c) 2009, Adrian Moser
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  * Neither the name of the author nor the
 *  names of its contributors may be used to endorse or promote products
 *  derived from this software without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL AUTHOR BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ch.eskaton.yajpg.api;

/**
 * Abstract base class for nodes in a syntax tree.
 */
public abstract class Node {

	/**
	 * Type of the node. Concrete types must be defined in subclasses.
	 */
	protected int nodeType;

	protected Node lNode;

	protected Node rNode;

	/**
	 * Returns the node type.
	 * 
	 * @return The node type
	 */
	public int getNodeType() {
		return nodeType;
	}

	/**
	 * Returns the left node.
	 * 
	 * @return The left node.
	 */
	public Node getLNode() {
		return lNode;
	}

	/**
	 * Returns the right node.
	 * 
	 * @return The right node.
	 */
	public Node getRNode() {
		return rNode;
	}

	/**
	 * Traverses a syntax tree and calls a {@link NodeVisitor} on every node.
	 * 
	 * @param v
	 *            A visitor
	 * @throws Exception
	 */
	public void traverse(NodeVisitor v) {
		if (getLNode() != null)
			getLNode().traverse(v);
		v.visit(this);
		if (getRNode() != null)
			getRNode().traverse(v);
	}

	/**
	 * Traverses a syntax tree in post-order and calls a {@link NodeVisitor} on
	 * every node.
	 * 
	 * @param v
	 *            A visitor
	 * @throws Exception
	 */
	public void traversePostOrder(NodeVisitor v) {
		if (getLNode() != null)
			getLNode().traversePostOrder(v);
		if (getRNode() != null)
			getRNode().traversePostOrder(v);
		v.visit(this);
	}

	/**
	 * Traverses a syntax tree in post-order and calls a {@link NodeVisitor}
	 * which may throw an exception on every node.
	 * 
	 * @param v
	 *            A visitor
	 * @throws Exception
	 */
	public void traversePostOrderWithEx(NodeVisitor v) throws Exception {
		if (getLNode() != null)
			getLNode().traversePostOrderWithEx(v);
		if (getRNode() != null)
			getRNode().traversePostOrderWithEx(v);
		v.visitWithException(this);
	}

	/**
	 * Prints the syntax tree to stdout.
	 * 
	 * @param level
	 *            The level of the node for indentation
	 */
	public final void print(int level) {
		for (int i = 0; i < level; i++) {
			System.out.print(" ");
		}

		doPrint();
		level++;

		if (lNode != null) {
			lNode.print(level);
		}
		if (rNode != null) {
			rNode.print(level);
		}
	}

	/**
	 * Does the actual printing of a node.
	 */
	public abstract void doPrint();

}
