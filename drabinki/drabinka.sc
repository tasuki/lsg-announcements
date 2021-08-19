import ammonite.ops._

// data

val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

case class Elimination(
  label: String,
  participants: Int,
  double: Boolean,
  rounds: Int,
  verticalSteps: Int,
  drops: Seq[Seq[Player]]
)
case class Player(label: Option[String], round: Int, topOffset: Int)
case class Match(label: Option[String], p1: Player, p2: Player) {
  val winner: Player = Player(None, math.max(p1.round, p2.round) + 1, (p1.topOffset + p2.topOffset) / 2)
}

// functions

def getPower(p: Int): Int =
  if (p > 2) 1 + getPower(p / 2)
  else 1

def seedPlayers(players: Seq[Int], step: Int, steps: Int): Seq[Int] =
  if (step > steps) players
  else {
    val magic = scala.math.pow(2, step).toInt + 1
    val newPlayers = players.flatMap(p => Seq(p, magic - p))
    seedPlayers(newPlayers, step + 1, steps)
  }

def playOff(players: Seq[Player], round: Int, printLabel: Boolean): Seq[Match] = {
  players.sortBy(_.topOffset).sliding(2, 2).zipWithIndex.flatMap {
    case (List(p1, p2), i) => {
      val label =
        if (printLabel) Some(round.toString + alphabet(i))
        else None
      Some(Match(label, p1, p2))
    }
    case _ => None
  }.toSeq
}

def playerMatch(
  ps: Seq[Player],
  round: Int,
  printLabel: Boolean,
  drops: Seq[Seq[Player]],
): (Seq[Match], Seq[Seq[Player]]) = {
  val players = ps ++ drops.headOption.getOrElse(Nil)
  val matches = playOff(players, round, printLabel)
  val winners = matches.map(_.winner)

  if (winners.length <= 1 && drops.flatten.length < 1) {
    if (winners.length == 0) (matches, Seq(players))
    else (matches, Seq(winners))
  } else {
    val (matches1, winners1) = playerMatch(winners, round + 1, printLabel, drops.drop(1))
    (matches1 ++ matches, winners1.prepended(winners))
  }
}



// view

case class SvgContext(height: Int, width: Int, padding: Int, e: Elimination) {
  val contentHeight = height - padding * 2
  val contentWidth = width - padding * 2
  val playerWidth = contentWidth / (e.rounds + 1)
  val fontStyle = "font-size: 40; font-family: Fira Mono;"
  val pathStyle = "stroke: black; stroke-width: 4; stroke-linecap: round;"

  def getPlayerCoords(p: Player): (Int, Int) = {
    val x = 40 + padding + ((p.round - 1) * contentWidth) / (e.rounds + 1)
    val y = -40 + padding + (p.topOffset * contentHeight) / e.verticalSteps
    (x, y)
  }

  def getPlayerPath(p: Player): scala.xml.Elem = {
    val (x, y) = getPlayerCoords(p)
    val text = p.label match {
      case Some(label) => <text x={(x - 5).toString} y={y.toString}
            text-anchor="end"
            style={fontStyle}>{label}</text>
      case None => xml.Text("")
    }

    <g>
      {text}
      <path d={s"M $x,$y h $playerWidth"} style={pathStyle}/>
    </g>
  }

  def getMatchPath(m: Match): scala.xml.Elem = {
    val (x1, y1) = getPlayerCoords(m.p1)
    val (x2, y2) = getPlayerCoords(m.p2)
    val (mx, my) = getPlayerCoords(m.winner)
    val text = m.label match {
      case Some(label) => <text x={(mx - 10).toString} y={(my + 14).toString}
            text-anchor="end"
            style={fontStyle}>{label}</text>
      case _ => xml.Text("")
    }
    val extend =
      if (x2 > x1) {
        <path d={s"M ${x1+playerWidth},${y1} h ${x2 - x1}"} style={pathStyle}/>
      } else xml.Text("")

    <g>
      {extend} {text}
      <path d={s"M ${x2+playerWidth},${y1} L ${x2+playerWidth},${y2}"} style={pathStyle}/>
    </g>
  }
}


def draw(e: Elimination): scala.xml.Elem = {
  val twoPower = getPower(e.participants)
  val seed = seedPlayers(Seq(1), 1, twoPower)
  val initialPlayers: Seq[Player] = seed.zipWithIndex.map {
    case (label, i) => Player(Some(label.toString), 1, 2+2*i)
  }

  val (players: Seq[Player], matches: Seq[Match]) =
    if (e.double) {
      val (winnermatches, winners) = playerMatch(initialPlayers, 1, true, Nil)
      val (losermatches, losers) = playerMatch(Nil, 2, false, e.drops)

      val finale: Seq[Match] = playOff(winners.last ++ losers.last, e.rounds, false)
      val winner: Seq[Player] = finale.map(_.winner)

      (initialPlayers ++ winners.flatten ++ e.drops.flatten ++ losers.flatten ++ winner,
      winnermatches ++ losermatches ++ finale)
    } else {
      val (winnermatches, winners) = playerMatch(initialPlayers, 1, false, Nil)
      (initialPlayers ++ winners.flatten,
      winnermatches)
    }

  val context = SvgContext(2100, 2970, 150, e)

  <svg viewBox={s"0 0 ${context.width} ${context.height}"} width="297mm" height="210mm" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
    <rect x="0" y="0" width={context.width.toString} height={context.height.toString} fill="white" />
    { players.map(context.getPlayerPath) }
    { matches.map(context.getMatchPath) }
  </svg>
}

def process(e: Elimination) = {
  val pp = new scala.xml.PrettyPrinter(100, 2)
  write.over(pwd/e.label, pp.format(draw(e)))
}

val drops8 = Seq(
  Seq(
    Player(Some("1A"), 2, 20),
    Player(Some("1B"), 2, 22),
    Player(Some("1C"), 2, 26),
    Player(Some("1D"), 2, 28),
  ),
  Seq(
    Player(Some("2B"), 3, 19),
    Player(Some("2A"), 3, 25),
  ),
  Seq(),
  Seq(
    Player(Some("3A"), 5, 18),
  ),
)

val drops16 = Seq(
  Seq(
    Player(Some("1A"), 2, 34),
    Player(Some("1B"), 2, 36),
    Player(Some("1E"), 2, 39),
    Player(Some("1F"), 2, 41),
    Player(Some("1C"), 2, 44),
    Player(Some("1D"), 2, 46),
    Player(Some("1G"), 2, 49),
    Player(Some("1H"), 2, 51),
  ),
  Seq(
    Player(Some("2B"), 3, 33),
    Player(Some("2D"), 3, 38),
    Player(Some("2A"), 3, 43),
    Player(Some("2C"), 3, 48),
  ),
  Seq(
    Player(Some("3B"), 4, 32),
    Player(Some("3A"), 4, 37),
  ),
  Seq(
    Player(Some("4A"), 5, 42),
  ),
)

val drops32 = Seq(
  Seq(
    Player(Some("1A"), 2, 66),
    Player(Some("1B"), 2, 68),
    Player(Some("1C"), 2, 70),
    Player(Some("1D"), 2, 72),
    Player(Some("1I"), 2, 74),
    Player(Some("1J"), 2, 76),
    Player(Some("1K"), 2, 78),
    Player(Some("1L"), 2, 80),
    Player(Some("1E"), 2, 82),
    Player(Some("1F"), 2, 84),
    Player(Some("1G"), 2, 86),
    Player(Some("1H"), 2, 88),
    Player(Some("1M"), 2, 90),
    Player(Some("1N"), 2, 92),
    Player(Some("1O"), 2, 94),
    Player(Some("1P"), 2, 96),
  ),
  Seq(
    Player(Some("2C"), 3, 65),
    Player(Some("2D"), 3, 69),
    Player(Some("2G"), 3, 73),
    Player(Some("2H"), 3, 77),
    Player(Some("2E"), 3, 81),
    Player(Some("2F"), 3, 85),
    Player(Some("2A"), 3, 89),
    Player(Some("2B"), 3, 93),
  ),
  Seq(),
  Seq(
    Player(Some("3C"), 5, 64),
    Player(Some("3A"), 5, 72),
    Player(Some("3D"), 5, 80),
    Player(Some("3B"), 5, 88),
  ),
  Seq(
    Player(Some("4A"), 6, 63),
    Player(Some("4B"), 6, 71),
  ),
  Seq(
    Player(Some("5A"), 7, 80),
  ),
)

Seq(
  Elimination("single-elim-08.svg",  8, false, 3, 18, Nil),
  Elimination("single-elim-16.svg", 16, false, 4, 32, Nil),
  Elimination("single-elim-32.svg", 32, false, 5, 64, Nil),

  Elimination("double-elim-08.svg",  8,  true, 6,  32, drops8),
  Elimination("double-elim-16.svg", 16,  true, 7,  52, drops16),
  Elimination("double-elim-32.svg", 32,  true, 9, 100, drops32),
).map(process)
