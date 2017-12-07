package encry.transaction

import java.io.File
import java.nio.ByteBuffer

import akka.actor.ActorRef
import encry.modifiers.mempool.EncryPersistentNodeViewModifier
import scorex.core.VersionTag
import scorex.core.transaction.box.Box
import scorex.core.transaction.box.proposition.{Proposition, PublicKey25519Proposition}
import scorex.core.utils.ScorexLogging
import scorex.core.transaction.state.{BoxStateChanges, Insertion, MinimalState, Removal}
import scorex.crypto.authds.ADDigest
import scorex.crypto.encode.Base16

import scala.util.Try

/**
  * Implementation of minimal state concept in Scorex. Minimal state (or just state from now) is some data structure
  * enough to validate a new blockchain element(e.g. block).
  * State in Ergo could be UTXO, like in Bitcoin or just a single digest. If the state is about UTXO, transaction set
  * of a block could be verified with no help of additional data. If the state is about just a digest, then proofs for
  * transformations of UTXO set presented in form of authenticated dynamic dictionary are needed to check validity of
  * a transaction set (see https://eprint.iacr.org/2016/994 for details).
  */

trait EncryMinimalState[EMS <: MinimalState[EncryPersistentNodeViewModifier, EMS]]
  extends MinimalState[EncryPersistentNodeViewModifier, EMS] with ScorexLogging {

  self: EMS =>

  // Extracts ordered sequence of operations to apply on UNXO (current state) from the set of transactions.
  def boxChanges(txs: Seq[EncryBaseTransaction]) : BoxStateChanges[PublicKey25519Proposition.type,
    PublicKey25519NoncedBox] = BoxStateChanges[PublicKey25519Proposition.type , PublicKey25519NoncedBox](
    txs.flatMap { tx =>
      tx.boxIdsToOpen.map(id => Removal[PublicKey25519Proposition.type, PublicKey25519NoncedBox](id)) ++
      tx.newBoxes.map(bx => Insertion[PublicKey25519Proposition.type, PublicKey25519NoncedBox](bx))
  })

  override def version: VersionTag

  override def applyModifier(mod: EncryPersistentNodeViewModifier): Try[EMS]

  override def rollbackTo(version: VersionTag): Try[EMS]

  def rollbackVersions: Iterable[VersionTag]

  override type NVCT = this.type

}


object EncryMinimalState extends ScorexLogging {

  val boxSize: Int = PublicKey25519NoncedBoxSerializer.length

//  def stateDir(settings: ?Settings) = new File(s"${settings.directory}/state")

//  def generateGenesisUtxoState(stateDir: File, nodeViewHolderRef: Option[ActorRef]): (UtxoState, BoxHolder) = {
//    log.info("Generating genesis UTXO state")
//    lazy val genesisSeed = Long.MaxValue
//    lazy val rndGen = new scala.util.Random(genesisSeed)
//    lazy val initialBoxesNumber = 10000
//
//    lazy val initialBoxes: Seq[PublicKey25519NoncedBox] =
//      (1 to initialBoxesNumber).map(_ => PublicKey25519NoncedBox(nonce = rndGen.nextLong(), value = 10000))
//
//    val bh = BoxHolder(initialBoxes)
//
//    UtxoState.fromBoxHolder(bh, stateDir, nodeViewHolderRef).ensuring(us => {
//      log.info("Genesis UTXO state generated")
//      us.rootHash.sameElements(afterGenesisStateDigest) && us.version.sameElements(genesisStateVersion)
//    }) -> bh
//  }
//
//  def generateGenesisDigestState(stateDir: File, settings: NodeConfigurationSettings): DigestState = {
//    DigestState.create(Some(genesisStateVersion), Some(afterGenesisStateDigest), stateDir, settings).get //todo: .get
//  }
//
//  val preGenesisStateDigest: ADDigest = ADDigest @@ Array.fill(32)(0: Byte)
//  //33 bytes
//  //TODO replace to Algos.decode
//  val afterGenesisStateDigestHex: String = "f2343e160d4e42a83a87ea1a2f56b6fa2046ab8146c5e61727c297be578da0f510"
//  val afterGenesisStateDigest: ADDigest = ADDigest @@ Base16.decode(afterGenesisStateDigestHex)
//
//  lazy val genesisStateVersion: VersionTag = VersionTag @@ Algos.hash(afterGenesisStateDigest.tail)
//
//  def readOrGenerate(settings: ErgoSettings, nodeViewHolderRef: Option[ActorRef]): Option[ErgoState[_]] = {
//    val dir = stateDir(settings)
//    dir.mkdirs()
//
//    if (dir.listFiles().isEmpty) {
//      None
//    } else {
//      //todo: considering db state
//      if (settings.nodeSettings.ADState) DigestState.create(None, None, dir, settings.nodeSettings).toOption
//      else Some(UtxoState.create(dir, nodeViewHolderRef))
//    }
//  }
}
